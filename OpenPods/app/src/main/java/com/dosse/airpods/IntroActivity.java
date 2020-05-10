package com.dosse.airpods;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class IntroActivity extends AppCompatActivity {

    private Timer timer;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // Allow button clicked, ask for permissions
        findViewById(R.id.allowBtn).setOnClickListener(view -> {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1); // Location (for BLE)
            // Run in background
            try {
                if (!Objects.requireNonNull(getSystemService(PowerManager.class)).isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent intent = new Intent();
                    getSystemService(Context.POWER_SERVICE);
                    /* This should not be used as it violates the Play Store Content Policy (https://developer.android.com/training/monitoring-device-state/doze-standby.html) */
                    // intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            } catch (Throwable ignored) { }
        });

        // Wait for permissions to be granted.
        // When they are granted, go to MainActivity.
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run () {
                boolean ok = true;

                try {
                    if (!Objects.requireNonNull(getSystemService(PowerManager.class)).isIgnoringBatteryOptimizations(getPackageName()))
                        ok = false;
                } catch (Throwable ignored) { }

                if (ContextCompat.checkSelfPermission(IntroActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    ok = false;

                if (ok) {
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

}
