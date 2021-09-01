package com.dosse.airpods.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class SharedPreferencesUtils {

    public static boolean isSavingBattery (Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("batterySaver", false);
    }

}
