package com.dosse.airpods;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Starter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context,PodsService.class));
    }
}
