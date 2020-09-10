package com.dosse.airpods;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class PodsWidget extends AppWidgetProvider {

    static boolean showBackground;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        int leftStatus = PodsService.leftStatus, rightStatus = PodsService.rightStatus, caseStatus = PodsService.caseStatus;
        boolean chargeL = PodsService.chargeL, chargeR = PodsService.chargeR, chargeCase = PodsService.chargeCase;

        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.pods_widget);

        if (showBackground) {
            widget.setViewVisibility(R.id.background, View.VISIBLE);
        } else {
            widget.setViewVisibility(R.id.background, View.GONE);
        }

        if (PodsService.model.equals(PodsService.MODEL_AIRPODS_NORMAL)) {
            widget.setImageViewResource(R.id.leftPodImg, leftStatus <= 10 ? R.drawable.pod : R.drawable.pod_disconnected);
            widget.setImageViewResource(R.id.rightPodImg, rightStatus <= 10 ? R.drawable.pod : R.drawable.pod_disconnected);
            widget.setImageViewResource(R.id.podCaseImg, caseStatus <= 10 ? R.drawable.pod_case : R.drawable.pod_case_disconnected);
        } else if (PodsService.model.equals(PodsService.MODEL_AIRPODS_PRO)) {
            widget.setImageViewResource(R.id.leftPodImg, leftStatus <= 10 ? R.drawable.podpro : R.drawable.podpro_disconnected);
            widget.setImageViewResource(R.id.rightPodImg, rightStatus <= 10 ? R.drawable.podpro : R.drawable.podpro_disconnected);
            widget.setImageViewResource(R.id.podCaseImg, caseStatus <= 10 ? R.drawable.podpro_case : R.drawable.podpro_case_disconnected);
        }

        if (System.currentTimeMillis() - PodsService.lastSeenConnected < PodsService.TIMEOUT_CONNECTED) {
            widget.setViewVisibility(R.id.leftPodText, View.VISIBLE);
            widget.setViewVisibility(R.id.rightPodText, View.VISIBLE);
            widget.setViewVisibility(R.id.podCaseText, View.VISIBLE);
            widget.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
            widget.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
            widget.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);

            String podText_Left = (leftStatus == 10) ? "100%" : ((leftStatus < 10) ? ((leftStatus * 10 + 5) + "%") : "");
            String podText_Right = (rightStatus == 10) ? "100%" : ((rightStatus < 10) ? ((rightStatus * 10 + 5) + "%") : "");
            String podText_Case = (caseStatus == 10) ? "100%" : ((caseStatus < 10) ? ((caseStatus * 10 + 5) + "%") : "");

            widget.setTextViewText(R.id.leftPodText, podText_Left);
            widget.setTextViewText(R.id.rightPodText, podText_Right);
            widget.setTextViewText(R.id.podCaseText, podText_Case);

            widget.setImageViewResource(R.id.leftBatImg, chargeL ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);
            widget.setImageViewResource(R.id.rightBatImg, chargeR ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);
            widget.setImageViewResource(R.id.caseBatImg, chargeCase ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);

            widget.setViewVisibility(R.id.leftBatImg, ((chargeL && leftStatus <= 10) || (leftStatus <= 1) ? View.VISIBLE : View.GONE));
            widget.setViewVisibility(R.id.rightBatImg, ((chargeR && rightStatus <= 10) || (rightStatus <= 1) ? View.VISIBLE : View.GONE));
            widget.setViewVisibility(R.id.caseBatImg, ((chargeCase && caseStatus <= 10) || (caseStatus <= 1) ? View.VISIBLE : View.GONE));
        } else {
            widget.setViewVisibility(R.id.leftPodText, View.INVISIBLE);
            widget.setViewVisibility(R.id.rightPodText, View.INVISIBLE);
            widget.setViewVisibility(R.id.podCaseText, View.INVISIBLE);
            widget.setViewVisibility(R.id.leftBatImg, View.GONE);
            widget.setViewVisibility(R.id.rightBatImg, View.GONE);
            widget.setViewVisibility(R.id.caseBatImg, View.GONE);
            widget.setViewVisibility(R.id.leftPodUpdating, View.VISIBLE);
            widget.setViewVisibility(R.id.rightPodUpdating, View.VISIBLE);
            widget.setViewVisibility(R.id.podCaseUpdating, View.VISIBLE);
        }

        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        PodsService.isWidgetActive = true;
    }

    @Override
    public void onDisabled(Context context) {
        PodsService.isWidgetActive = false;
    }
}

