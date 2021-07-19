package com.dosse.airpods;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.util.Objects;

public class PermissionUtils {

    public static boolean getBatteryOptimizationsPermission (Context context) {
        try {
            return Objects.requireNonNull(context.getSystemService(PowerManager.class)).isIgnoringBatteryOptimizations(context.getPackageName());
        } catch (Throwable ignored) {
            return true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public static boolean getBluetoothPermissions (Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean getFineLocationPermission (Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean getBackgroundLocationPermission (Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkAllPermissions (Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            return getBatteryOptimizationsPermission(context) && getFineLocationPermission(context) && getBackgroundLocationPermission(context) && getBluetoothPermissions(context);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            return getBatteryOptimizationsPermission(context) && getFineLocationPermission(context) && getBackgroundLocationPermission(context);
        else
            return getBatteryOptimizationsPermission(context) && getFineLocationPermission(context);
    }

}
