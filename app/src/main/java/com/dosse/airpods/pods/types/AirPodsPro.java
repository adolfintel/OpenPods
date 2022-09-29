package com.dosse.airpods.pods.types;

import com.dosse.airpods.R;

public class AirPodsPro extends RegularPods {

    public AirPodsPro (Pod leftPod, Pod rightPod, Pod casePod) {
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
        return MODEL_AIRPODS_PRO;
    }

}