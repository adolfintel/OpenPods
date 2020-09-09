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

        // Instruct the widget manager to update the widget
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

