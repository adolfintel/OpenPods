package com.dosse.airpods;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

/**
 * Implementation of App Widget functionality.
 */
public class PodsWidget extends AppWidgetProvider {

    static boolean showBackground;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        int leftStatus = PodsService.leftStatus, rightStatus = PodsService.rightStatus, caseStatus = PodsService.caseStatus;
        boolean chargeL = PodsService.chargeL, chargeR = PodsService.chargeR, chargeCase = PodsService.chargeCase;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.pods_widget);

        if (showBackground) {
            views.setViewVisibility(R.id.background, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.background, View.GONE);
        }

        if (leftStatus != 15) {
            views.setProgressBar(R.id.leftPodProgress, 100, leftStatus * 10, false);
            String podText_Left = (leftStatus == 10) ? "100%" : ((leftStatus < 10) ? ((leftStatus * 10 + 5) + "%") : "");
            views.setTextViewText(R.id.leftPodText, podText_Left);
            views.setViewVisibility(R.id.left, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.left, View.GONE);
        }

        views.setImageViewResource(R.id.leftPodBat, chargeL ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);
        views.setImageViewResource(R.id.rightPodBat, chargeR ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);
        views.setImageViewResource(R.id.caseBat, chargeCase ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);

        views.setViewVisibility(R.id.leftPodBat, chargeL ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.rightPodBat, chargeR ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.caseBat, chargeCase ? View.VISIBLE : View.GONE);

        if (PodsService.model.equals(PodsService.MODEL_AIRPODS_NORMAL)) {
            views.setImageViewResource(R.id.leftPodImg, R.drawable.pod);
            views.setImageViewResource(R.id.rightPodImg, R.drawable.pod);
            views.setImageViewResource(R.id.caseImg, R.drawable.pod_case);
        } else if (PodsService.model.equals(PodsService.MODEL_AIRPODS_PRO)) {
            views.setImageViewResource(R.id.leftPodImg, R.drawable.podpro);
            views.setImageViewResource(R.id.rightPodImg, R.drawable.podpro);
            views.setImageViewResource(R.id.caseImg, R.drawable.podpro_case);
        }

        if (rightStatus != 15) {
            views.setProgressBar(R.id.rightPodProgress, 100, rightStatus * 10, false);
            String podText_Right = (rightStatus == 10) ? "100%" : ((rightStatus < 10) ? ((rightStatus * 10 + 5) + "%") : "");
            views.setTextViewText(R.id.rightPodText, podText_Right);
            views.setViewVisibility(R.id.right, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.right, View.GONE);
        }

        if (caseStatus != 15) {
            views.setProgressBar(R.id.caseProgress, 100, caseStatus * 10, false);
            String podText_Case = (caseStatus == 10) ? "100%" : ((caseStatus < 10) ? ((caseStatus * 10 + 5) + "%") : "");
            views.setTextViewText(R.id.caseText, podText_Case);
            views.setViewVisibility(R.id.casePods, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.casePods, View.GONE);
        }

        if (leftStatus == 15 && rightStatus == 15 && caseStatus == 15) {
            views.setViewVisibility(R.id.title, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.title, View.GONE);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        showBackground = prefs.getBoolean("widgetBackground", false);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }
}

