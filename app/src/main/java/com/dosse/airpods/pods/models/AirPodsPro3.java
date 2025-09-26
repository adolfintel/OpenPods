package com.dosse.airpods.pods.models;

import com.dosse.airpods.R;
import com.dosse.airpods.pods.Pod;

public class AirPodsPro3 extends AirPodsPro {
    public AirPodsPro3(Pod leftPod, Pod rightPod, Pod casePod) {
        super(leftPod, rightPod, casePod);
    }

    @Override
    public int getLeftDrawable() {
        return getPod(LEFT).isConnected() ? R.drawable.podpro3 : R.drawable.podpro3_disconnected;
    }

    @Override
    public int getRightDrawable() {
        return getPod(RIGHT).isConnected() ? R.drawable.podpro3 : R.drawable.podpro3_disconnected;
    }

    @Override
    public String getModel() {
        return Constants.MODEL_AIRPODS_PRO_3;
    }
}
