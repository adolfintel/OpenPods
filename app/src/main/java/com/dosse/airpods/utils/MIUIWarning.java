package com.dosse.airpods.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;

import com.dosse.airpods.R;

import java.lang.reflect.Method;

public class MIUIWarning {
    public static void show(Context context) {
        Context appContext = context.getApplicationContext();

        try {
            String miuiVersion = getMiuiVersion();
            if (miuiVersion == null || miuiVersion.isEmpty()) {
                return;
            }

            try {
                appContext.openFileInput("miuiwarn").close();
            } catch (Throwable ignored) {
                showAlertDialog(appContext);
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressLint("PrivateApi")
    private static String getMiuiVersion() throws ReflectiveOperationException {
        Class<?> systemPropertiesClazz = Class.forName("android.os.SystemProperties");
        Method systemPropertiesGet = systemPropertiesClazz.getMethod("get", String.class);
        systemPropertiesGet.setAccessible(true);

        return (String)systemPropertiesGet.invoke(null, "ro.miui.ui.version.code");
    }

    private static void showAlertDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.miui_warning);
        builder.setMessage(R.string.miui_warning_desc);
        builder.setNeutralButton(R.string.miui_warning_continue, (dialog, which) -> dialog.dismiss());
        builder.setOnDismissListener(dialog -> {
            try {
                context.openFileOutput("miuiwarn", Context.MODE_PRIVATE).close();
            } catch (Throwable ignored2) {
            }
        });
        builder.show();
    }
}
