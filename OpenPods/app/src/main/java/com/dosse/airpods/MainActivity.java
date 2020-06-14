package com.dosse.airpods;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if Bluetooth LE is available on this device. If not, show an error
        BluetoothAdapter btAdapter = ((BluetoothManager)Objects.requireNonNull(getSystemService(Context.BLUETOOTH_SERVICE))).getAdapter();
        if (btAdapter == null || (btAdapter.isEnabled() && btAdapter.getBluetoothLeScanner() == null) || (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))) {
            startActivity(new Intent(MainActivity.this, NoBTActivity.class));
            finish();
            return;
        }

        // Check if all permissions have been granted
        boolean ok = true;

        try {
            if (!Objects.requireNonNull(getSystemService(PowerManager.class)).isIgnoringBatteryOptimizations(getPackageName()))
                ok = false;
        } catch (Throwable ignored) {
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ok = false;

        if (ok) {
            Starter.startPodsService(getApplicationContext());
            //Warn MIUI users that their rom has known issues
            try{
                Class<?> c = Class.forName("android.os.SystemProperties");
                String miuiVersion = (String) c.getMethod("get", String.class).invoke(c, "ro.miui.ui.version.code");
                if(miuiVersion!=null&&!miuiVersion.isEmpty()){
                    try{
                        getApplicationContext().openFileInput("miuiwarn").close();
                    }catch (Throwable ignored){
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(R.string.miui_warning);
                        builder.setMessage(R.string.miui_warning_desc);
                        builder.setNeutralButton(R.string.miui_warning_continue, (dialog, which) -> {
                            try {
                                getApplicationContext().openFileOutput("miuiwarn", Context.MODE_PRIVATE).close();
                            } catch (Throwable t) {}
                        });
                        builder.show();
                    }
                }
            }catch(Throwable ignored){}
        } else {
            startActivity(new Intent(MainActivity.this, IntroActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume () {
        super.onResume();
        try {
            getApplicationContext().openFileInput("hidden").close();
            finish();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_ab_settings) {
            startActivity(new Intent(this, SettingsActivity.class)); // Settings icon clicked
            return true;
        }
        return false;
    }

}
