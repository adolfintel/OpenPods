package com.dosse.airpods.pods.models;

import com.dosse.airpods.R;
import com.dosse.airpods.pods.Pod;

public class AirPodsPro2 extends RegularPods {

    public AirPodsPro2 (Pod leftPod, Pod rightPod, Pod casePod) {
        super(leftPod, rightPod, casePod);
    }

    @Override
    public int getLeftDrawable () {
        return getPod(LEFT).isConnected() ? R.drawable.podpro : R.drawable.podpro_disconnected;
    }

    @Override
    public int getRightDrawable () {
        return getPod(RIGHT).isConnected() ? R.drawable.podpro : R.drawable.podpro_disconnected;
    }

    @Override
    public int getCaseDrawable () {
        return getPod(CASE).isConnected() ? R.drawable.podpro_case : R.drawable.podpro_case_disconnected;
    }

    @Override
    public String getModel () {
        return Constants.MODEL_AIRPODS_PRO_2;
    }

}