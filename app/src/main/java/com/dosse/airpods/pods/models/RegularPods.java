package com.dosse.airpods.pods.models;

import com.dosse.airpods.R;
import com.dosse.airpods.pods.Pod;

import java.util.Locale;

public class RegularPods implements IPods {

    public static final int LEFT = 0, RIGHT = 1, CASE = 2;

    private final Pod[] pods;

    public RegularPods (Pod leftPod, Pod rightPod, Pod casePod) {
        this.pods = new Pod[] {leftPod, rightPod, casePod};
    }

    public Pod getPod (int pos) {
        return pods[pos];
    }

    public String getParsedStatus (int pos) {
        return pods[pos].parseStatus();
    }

    public int getInEarVisibility (int pos) {
        return pods[pos].inEarVisibility();
    }

    public int getBatImgVisibility (int pos) {
        return pods[pos].batImgVisibility();
    }

    public int getBatImgSrcId (int pos) {
        return pods[pos].batImgSrcId();
    }

    public int getLeftDrawable () {
        return getPod(LEFT).isConnected() ? R.drawable.pod : R.drawable.pod_disconnected;
    }

    public int getRightDrawable () {
        return getPod(RIGHT).isConnected() ? R.drawable.pod : R.drawable.pod_disconnected;
    }

    public int getCaseDrawable () {
        return getPod(CASE).isConnected() ? R.drawable.pod_case : R.drawable.pod_case_disconnected;
    }

    @Override
    public String getModel () {
        return Constants.MODEL_UNKNOWN;
    }

    @Override
    public boolean isSingle () {
        return false;
    }

    @Override
    public boolean isDisconnected () {
        return pods[LEFT].isDisconnected() &&
                pods[RIGHT].isDisconnected() &&
                pods[CASE].isDisconnected();
    }

    @Override
    public String parseStatusForLogger () {
        return String.format(Locale.getDefault(), "Left: %d%s%s Right: %d%s%s Case: %d%s Model: %s",
                pods[LEFT].getStatus(), pods[LEFT].isCharging() ? "+" : "", pods[LEFT].isInEar() ? "$" : "",
                pods[RIGHT].getStatus(), pods[RIGHT].isCharging() ? "+" : "", pods[RIGHT].isInEar() ? "$" : "",
                pods[CASE].getStatus(), pods[CASE].isCharging() ? "+" : "", getModel());
    }

}