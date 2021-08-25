package com.dosse.airpods.notification;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.dosse.airpods.R;
import com.dosse.airpods.pods.PodsStatus;
import com.dosse.airpods.pods.data.IPods;
import com.dosse.airpods.pods.data.RegularPods;
import com.dosse.airpods.pods.data.SinglePods;
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

        IPods airpods = status.getAirpods();
        boolean single = airpods.isSingle();

        for (RemoteViews notification : notificationArr) {
            if (!single) {
                notification.setImageViewResource(R.id.leftPodImg, ((RegularPods)airpods).getLeftDrawable());
                notification.setImageViewResource(R.id.rightPodImg, ((RegularPods)airpods).getRightDrawable());
                notification.setImageViewResource(R.id.podCaseImg, ((RegularPods)airpods).getCaseDrawable());
            } else {
                notification.setImageViewResource(R.id.podCaseImg, ((SinglePods)airpods).getDrawable());
            }

            notification.setViewVisibility(R.id.leftPod, single ? View.GONE : View.VISIBLE);
            notification.setViewVisibility(R.id.rightPod, single ? View.GONE : View.VISIBLE);
        }

        if (isFreshStatus(status)) for (RemoteViews notification : notificationArr) {
            notification.setViewVisibility(R.id.leftPodText, View.VISIBLE);
            notification.setViewVisibility(R.id.rightPodText, View.VISIBLE);
            notification.setViewVisibility(R.id.podCaseText, View.VISIBLE);
            notification.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
            notification.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
            notification.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);

            if (!single) {
                RegularPods regularPods = (RegularPods)airpods;

                notification.setTextViewText(R.id.leftPodText, regularPods.getParsedStatus(RegularPods.LEFT));
                notification.setTextViewText(R.id.rightPodText, regularPods.getParsedStatus(RegularPods.RIGHT));
                notification.setTextViewText(R.id.podCaseText, regularPods.getParsedStatus(RegularPods.CASE));

                notification.setImageViewResource(R.id.leftBatImg, regularPods.getBatImgSrcId(RegularPods.LEFT));
                notification.setImageViewResource(R.id.rightBatImg, regularPods.getBatImgSrcId(RegularPods.RIGHT));
                notification.setImageViewResource(R.id.caseBatImg, regularPods.getBatImgSrcId(RegularPods.CASE));

                notification.setViewVisibility(R.id.leftBatImg, regularPods.getBatImgVisibility(RegularPods.LEFT));
                notification.setViewVisibility(R.id.rightBatImg, regularPods.getBatImgVisibility(RegularPods.RIGHT));
                notification.setViewVisibility(R.id.caseBatImg, regularPods.getBatImgVisibility(RegularPods.CASE));

                notification.setViewVisibility(R.id.leftInEarImg, regularPods.getInEarVisibility(RegularPods.LEFT));
                notification.setViewVisibility(R.id.rightInEarImg, regularPods.getInEarVisibility(RegularPods.RIGHT));
            } else {
                SinglePods singlePods = (SinglePods)airpods;

                notification.setTextViewText(R.id.podCaseText, singlePods.getParsedStatus());
                notification.setImageViewResource(R.id.caseBatImg, singlePods.getBatImgSrcId());
                notification.setViewVisibility(R.id.caseBatImg, singlePods.getBatImgVisibility());
            }
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

}