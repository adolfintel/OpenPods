package com.dosse.airpods;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A simple starter class that starts the service when the device is booted, or after an update
 */
public class Starter extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive (Context context, Intent intent) {
        startPodsService(context);
    }

    public static void startPodsService (Context context) {
        context.startService(new Intent(context, PodsService.class));
    }

    public static void restartPodsService (Context context) {
        context.stopService(new Intent(context, PodsService.class));
        try {
            Thread.sleep(500);
        } catch (Throwable ignored) {
        }
        context.startService(new Intent(context, PodsService.class));
    }

}
