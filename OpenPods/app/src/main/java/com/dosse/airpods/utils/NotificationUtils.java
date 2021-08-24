package com.dosse.airpods.utils;

import static com.dosse.airpods.utils.ScannerUtils.isMax;

import android.view.View;

import com.dosse.airpods.R;

public class NotificationUtils {

    public static String statusToString (int status) {
        return (status == 10) ? "100%" : ((status < 10) ? ((status * 10 + 5) + "%") : "");
    }

    public static int batImgSrcId (boolean charge) {
        return charge ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp;
    }

    public static int batImgVisibility (boolean charge, int status) {
        return ((charge && status <= 10 && !isMax()) || status <= 1) ? View.VISIBLE : View.GONE;
    }

}
