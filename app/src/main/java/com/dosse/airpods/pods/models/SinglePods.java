package com.dosse.airpods.pods.models;

import com.dosse.airpods.pods.Pod;

import java.util.Locale;

public abstract class SinglePods implements IPods {

    public abstract int getDrawable ();

    private final Pod pod;

    public SinglePods (Pod pod) {
        this.pod = pod;
    }

    public Pod getPod () {
        return pod;
    }

    public String getParsedStatus () {
        return pod.parseStatus();
    }

    public int getBatImgVisibility () {
        return pod.batImgVisibility();
    }

    public int getBatImgSrcId () {
        return pod.batImgSrcId();
    }

    @Override
    public boolean isSingle () {
        return true;
    }

    @Override
    public boolean isDisconnected () {
        return pod.isDisconnected();
    }

    @Override
    public String parseStatusForLogger () {
        return String.format(Locale.getDefault(), "Battery: %d%s Model: %s", pod.getStatus(), pod.isCharging() ? "+" : "", getModel());
    }

}