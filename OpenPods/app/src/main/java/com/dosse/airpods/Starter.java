package com.dosse.airpods;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * A simple starter class that starts the service when the device is booted, or after an update
 */
public class Starter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context,PodsService.class));
    }
}
