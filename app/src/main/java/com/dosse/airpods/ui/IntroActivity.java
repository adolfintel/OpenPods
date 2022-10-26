package com.dosse.airpods.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.dosse.airpods.R;
import com.dosse.airpods.utils.PermissionUtils;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class IntroActivity extends AppCompatActivity {

    private Timer timer;
    private TextView msg;
    private Button btn;

    private static final int STEP_PERMISSION_BLUETOOTH = 1, STEP_PERMISSION_BATTERY_OPTIMIZATION = 2, STEP_PERMISSION_LOCATION = 3, STEP_PERMISSION_BACKGROUND_LOCATION = 4, STEP_PERMISSION_NOTIFICATION = 5;
    private static final int BLUETOOTH_REQUEST_CODE = 101, LOCATION_REQUEST_CODE = 102, BACKGROUND_LOCATION_REQUEST_CODE = 103, NOTIFICATION_REQUEST_CODE = 104;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        msg = findViewById(R.id.permsMsg);
        btn = findViewById(R.id.permsBtn);

        // Wait for permissions to be granted.
        // When they are granted, go to MainActivity.
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run () {
                initScreen();

                if (PermissionUtils.checkAllPermissions(IntroActivity.this)) {
                    timer.cancel();
                    startActivity(new Intent(IntroActivity.this, MainActivity.class));
                    finish();
                }
            }
        }, 0, 100);

    }

    // Activity destroyed (or screen rotated). destroy the timer too
    @Override
    protected void onDestroy () {
        super.onDestroy();
        if (timer != null)
            timer.cancel();
    }

    private int getPermissionState () {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (!PermissionUtils.getBluetoothPermissions(this))
            return STEP_PERMISSION_BLUETOOTH;
        if (!PermissionUtils.getBatteryOptimizationsPermission(this))
            return STEP_PERMISSION_BATTERY_OPTIMIZATION;
        if (!PermissionUtils.getFineLocationPermission(this))
            return STEP_PERMISSION_LOCATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) if (!PermissionUtils.getBackgroundLocationPermission(this))
            return STEP_PERMISSION_BACKGROUND_LOCATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) if (!PermissionUtils.getNotificationPermissions(this))
            return STEP_PERMISSION_NOTIFICATION;
        return 0;
    }

    @SuppressLint("BatteryLife")
    private void initScreen () {
        int currentStep = getPermissionState() - ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? 0 : 1);
        int numOfSteps = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? 5 : (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ? ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ? 4 : 3) : 2;

        runOnUiThread(() -> {
            switch (getPermissionState()) {
                case STEP_PERMISSION_BLUETOOTH:
                    msg.setText(String.format(Locale.getDefault(), "%s %d/%d: %s", getString(R.string.intro_step), currentStep, numOfSteps, getString(R.string.intro_bt_perm)));
                    btn.setOnClickListener(view -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            requestPermissions(new String[] {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_REQUEST_CODE);
                    });
                    break;
                case STEP_PERMISSION_BATTERY_OPTIMIZATION:
                    msg.setText(String.format(Locale.getDefault(), "%s %d/%d: %s", getString(R.string.intro_step), currentStep, numOfSteps, getString(R.string.intro_bat_perm)));
                    btn.setOnClickListener(view -> {
                        Intent intent = new Intent();
                        getSystemService(Context.POWER_SERVICE);
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    });
                    break;
                case STEP_PERMISSION_LOCATION:
                    msg.setText(String.format(Locale.getDefault(), "%s %d/%d: %s", getString(R.string.intro_step), currentStep, numOfSteps, getString(R.string.intro_loc1_perm)));
                    btn.setOnClickListener(view -> requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE)); // Location (for BLE)
                    break;
                case STEP_PERMISSION_BACKGROUND_LOCATION:
                    msg.setText(String.format(Locale.getDefault(), "%s %d/%d: %s", getString(R.string.intro_step), currentStep, numOfSteps, getString(R.string.intro_loc2_perm)));
                    btn.setOnClickListener(view -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) requestPermissions(new String[] {Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_REQUEST_CODE);
                    });
                    break;
                case STEP_PERMISSION_NOTIFICATION:
                    msg.setText(String.format(Locale.getDefault(), "%s %d/%d: %s", getString(R.string.intro_step), currentStep, numOfSteps, getString(R.string.intro_notif_perm)));
                    btn.setOnClickListener(view -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST_CODE);
                    });
            }
            btn.setText(String.format(Locale.getDefault(), "%s (%d/%d)", getString(R.string.intro_allow), currentStep, numOfSteps));
        });
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case BLUETOOTH_REQUEST_CODE:
                if (!hasAllPermissionsGranted(grantResults)) Toast.makeText(this, getString(R.string.msg_bt_perm), Toast.LENGTH_LONG).show();
                break;
            case LOCATION_REQUEST_CODE:
                if (!hasAllPermissionsGranted(grantResults)) Toast.makeText(this, getString(R.string.msg_loc1_perm), Toast.LENGTH_LONG).show();
                break;
            case BACKGROUND_LOCATION_REQUEST_CODE:
                if (!hasAllPermissionsGranted(grantResults)) Toast.makeText(this, getString(R.string.msg_loc2_perm), Toast.LENGTH_LONG).show();
                break;
            case NOTIFICATION_REQUEST_CODE:
                if (!hasAllPermissionsGranted(grantResults)) Toast.makeText(this, getString(R.string.msg_notif_perm), Toast.LENGTH_LONG).show();
                break;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasAllPermissionsGranted (@NonNull int[] grantResults) {
        for (int grantResult : grantResults)
            if (grantResult == PackageManager.PERMISSION_DENIED)
                return false;

        return true;
    }

}
