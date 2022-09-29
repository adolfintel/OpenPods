package com.dosse.airpods.pods;

import android.view.View;

import com.dosse.airpods.R;

public class Pod {

    public static final int DISCONNECTED_STATUS = 15;
    public static final int MAX_CONNECTED_STATUS = 10;
    public static final int LOW_BATTERY_STATUS = 1;

    private final int status;
    private final boolean charging;
    private final boolean inEar;

    public Pod (int status, boolean charging, boolean inEar) {
        this.status = status;
        this.charging = charging;
        this.inEar = inEar;
    }

    public int getStatus () {
        return status;
    }

    public String parseStatus () {
        return (status == MAX_CONNECTED_STATUS) ? "100%" : ((status < MAX_CONNECTED_STATUS) ? ((status * 10 + 5) + "%") : "");
    }

    public boolean isCharging () {
        return charging;
    }

    public boolean isInEar () {
        return inEar;
    }

    public boolean isConnected () {
        return status <= MAX_CONNECTED_STATUS;
    }

    public boolean isDisconnected () {
        return status == DISCONNECTED_STATUS;
    }

    public boolean isLowBattery () {
        return status <= LOW_BATTERY_STATUS;
    }

    public int inEarVisibility () {
        return inEar ? View.VISIBLE : View.INVISIBLE;
    }

    public int batImgVisibility () {
        return (charging && isConnected() || isLowBattery()) ? View.VISIBLE : View.GONE;
    }

    public int batImgSrcId () {
        return charging ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp;
    }

}