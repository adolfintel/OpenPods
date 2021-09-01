package com.dosse.airpods.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;

import com.dosse.airpods.R;

public class MIUIWarning {

    public static void show (Context context) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            String miuiVersion = (String)c.getMethod("get", String.class).invoke(c, "ro.miui.ui.version.code");
            if (miuiVersion != null && !miuiVersion.isEmpty()) {
                try {
                    context.getApplicationContext().openFileInput("miuiwarn").close();
                } catch (Throwable ignored) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.miui_warning);
                    builder.setMessage(R.string.miui_warning_desc);
                    builder.setNeutralButton(R.string.miui_warning_continue, (dialog, which) -> dialog.dismiss());
                    builder.setOnDismissListener(dialog -> {
                        try {
                            context.getApplicationContext().openFileOutput("miuiwarn", Context.MODE_PRIVATE).close();
                        } catch (Throwable ignored2) {
                        }
                    });
                    builder.show();
                }
            }
        } catch (Throwable ignored) {
        }
    }

}
