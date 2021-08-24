package com.dosse.airpods.notification;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.dosse.airpods.R;
import com.dosse.airpods.pods.Pod;
import com.dosse.airpods.pods.PodsStatus;
import com.dosse.airpods.utils.PermissionUtils;

public class NotificationBuilder {
    public static final String TAG = "AirPods";
    public static final long TIMEOUT_CONNECTED = 30000;
    public static final int NOTIFICATION_ID = 1;

    private final RemoteViews[] notificationArr, notificationLocation;
    private final Context mContext;
    private final NotificationCompat.Builder mBuilder;

    public NotificationBuilder (Context context) {
        mContext = context;

        notificationArr = new RemoteViews[] {new RemoteViews(context.getPackageName(), R.layout.status_big), new RemoteViews(context.getPackageName(), R.layout.status_small)};
        notificationLocation = new RemoteViews[] {new RemoteViews(context.getPackageName(), R.layout.location_disabled_big), new RemoteViews(context.getPackageName(), R.layout.location_disabled_small)};

        mBuilder = new NotificationCompat.Builder(context, TAG);
        mBuilder.setShowWhen(false);
        mBuilder.setOngoing(true);
        mBuilder.setSmallIcon(R.mipmap.notification_icon);
        mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    public Notification build (PodsStatus status) {

        // Apparently this restriction was removed ONLY in android Q
        if (PermissionUtils.getLocationPermissions(mContext) || Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            mBuilder.setCustomContentView(notificationArr[1]);
            mBuilder.setCustomBigContentView(notificationArr[0]);
        } else {
            mBuilder.setCustomContentView(notificationLocation[1]);
            mBuilder.setCustomBigContentView(notificationLocation[0]);
        }

        if (status.isAirpods()) for (RemoteViews notification : notificationArr) {
            notification.setImageViewResource(R.id.leftPodImg, status.getLeftPod().isConnected() ? R.drawable.pod : R.drawable.pod_disconnected);
            notification.setImageViewResource(R.id.rightPodImg, status.getRightPod().isConnected() ? R.drawable.pod : R.drawable.pod_disconnected);
            notification.setImageViewResource(R.id.podCaseImg, status.getCasePod().isConnected() ? R.drawable.pod_case : R.drawable.pod_case_disconnected);
        }
        else if (status.isAirpodsPro()) for (RemoteViews notification : notificationArr) {
            notification.setImageViewResource(R.id.leftPodImg, status.getLeftPod().isConnected() ? R.drawable.podpro : R.drawable.podpro_disconnected);
            notification.setImageViewResource(R.id.rightPodImg, status.getRightPod().isConnected() ? R.drawable.podpro : R.drawable.podpro_disconnected);
            notification.setImageViewResource(R.id.podCaseImg, status.getCasePod().isConnected() ? R.drawable.podpro_case : R.drawable.podpro_case_disconnected);
        }
        else if (status.isAirpodsMax()) for (RemoteViews notification : notificationArr) {
            notification.setImageViewResource(R.id.leftPodImg, status.getMaxPod().isConnected() ? R.drawable.podmax : R.drawable.podmax_disconnected);
        }

        for (RemoteViews notification : notificationArr) {
            notification.setViewVisibility(R.id.rightPod, status.isAirpodsMax() ? View.GONE : View.VISIBLE);
            notification.setViewVisibility(R.id.podCase, status.isAirpodsMax() ? View.GONE : View.VISIBLE);
        }

        if (isFreshStatus(status)) for (RemoteViews notification : notificationArr) {
            notification.setViewVisibility(R.id.leftPodText, View.VISIBLE);
            notification.setViewVisibility(R.id.rightPodText, View.VISIBLE);
            notification.setViewVisibility(R.id.podCaseText, View.VISIBLE);
            notification.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
            notification.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
            notification.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);

            notification.setTextViewText(R.id.leftPodText, status.isAirpodsMax() ? status.getMaxPod().parseStatus() : status.getLeftPod().parseStatus());
            notification.setTextViewText(R.id.rightPodText, status.getRightPod().parseStatus());
            notification.setTextViewText(R.id.podCaseText, status.getCasePod().parseStatus());

            notification.setImageViewResource(R.id.leftBatImg, batImgSrcId(status.isAirpodsMax() ? status.getMaxPod() : status.getLeftPod()));
            notification.setImageViewResource(R.id.rightBatImg, batImgSrcId(status.getRightPod()));
            notification.setImageViewResource(R.id.caseBatImg, batImgSrcId(status.getCasePod()));

            notification.setViewVisibility(R.id.leftBatImg, batImgVisibility(status.isAirpodsMax() ? status.getMaxPod() : status.getLeftPod()));
            notification.setViewVisibility(R.id.rightBatImg, batImgVisibility(status.getRightPod()));
            notification.setViewVisibility(R.id.caseBatImg, batImgVisibility(status.getCasePod()));

            notification.setViewVisibility(R.id.leftInEarImg, status.getLeftPod().isInEar() ? View.VISIBLE : View.INVISIBLE);
            notification.setViewVisibility(R.id.rightInEarImg, status.getRightPod().isInEar() ? View.VISIBLE : View.INVISIBLE);
        }
        else for (RemoteViews notification : notificationArr) {
            notification.setViewVisibility(R.id.leftPodText, View.INVISIBLE);
            notification.setViewVisibility(R.id.rightPodText, View.INVISIBLE);
            notification.setViewVisibility(R.id.podCaseText, View.INVISIBLE);
            notification.setViewVisibility(R.id.leftBatImg, View.GONE);
            notification.setViewVisibility(R.id.rightBatImg, View.GONE);
            notification.setViewVisibility(R.id.caseBatImg, View.GONE);
            notification.setViewVisibility(R.id.leftPodUpdating, View.VISIBLE);
            notification.setViewVisibility(R.id.rightPodUpdating, View.VISIBLE);
            notification.setViewVisibility(R.id.podCaseUpdating, View.VISIBLE);
            notification.setViewVisibility(R.id.leftInEarImg, View.INVISIBLE);
            notification.setViewVisibility(R.id.rightInEarImg, View.INVISIBLE);
        }

        return mBuilder.build();
    }

    private boolean isFreshStatus (PodsStatus status) {
        return System.currentTimeMillis() - status.getTimestamp() < TIMEOUT_CONNECTED;
    }

    private static int batImgSrcId (Pod pod) {
        return pod.isCharging() ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp;
    }

    private static int batImgVisibility (Pod pod) {
        return (pod.isCharging() && pod.isConnected() || pod.isLowBattery()) ? View.VISIBLE : View.GONE;
    }

}