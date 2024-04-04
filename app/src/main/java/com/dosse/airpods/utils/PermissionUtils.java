package com.dosse.airpods.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import androidx.annotation.RequiresApi;

import static androidx.core.content.ContextCompat.checkSelfPermission;

public class PermissionUtils {
    public static boolean getBatteryOptimizationsPermission(Context context) {
        return context.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(context.getPackageName());
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static boolean getNotificationPermissions(Context context) {
        return checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public static boolean getBluetoothPermissions(Context context) {
        return checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean getFineLocationPermission(Context context) {
        return checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean getBackgroundLocationPermission(Context context) {
        return checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean getLocationPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getFineLocationPermission(context) && getBackgroundLocationPermission(context);
        }

        return getFineLocationPermission(context);
    }

    public static boolean checkAllPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return getNotificationPermissions(context) && getBatteryOptimizationsPermission(context) && getLocationPermissions(context) && getBluetoothPermissions(context);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return getBatteryOptimizationsPermission(context) && getLocationPermissions(context);
        }

        return getBatteryOptimizationsPermission(context) && getLocationPermissions(context) && getBluetoothPermissions(context);
    }
}
