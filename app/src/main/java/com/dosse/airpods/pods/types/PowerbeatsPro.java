package com.dosse.airpods.pods.types;

import com.dosse.airpods.R;

public class PowerbeatsPro extends RegularPods {

    public PowerbeatsPro (Pod leftPod, Pod rightPod, Pod casePod) {
        super(leftPod, rightPod, casePod);
    }

    @Override
    public int getLeftDrawable () {
        return getPod(LEFT).isConnected() ? R.drawable.powerbeatspro : R.drawable.powerbeatspro_disconnected;
    }

    @Override
    public int getRightDrawable () {
        return getPod(RIGHT).isConnected() ? R.drawable.powerbeatspro : R.drawable.powerbeatspro_disconnected;
    }

    @Override
    public int getCaseDrawable () {
        return getPod(CASE).isConnected() ? R.drawable.powerbeatspro_case : R.drawable.powerbeatspro_case_disconnected;
    }

    @Override
    public String getModel () {
        return MODEL_POWERBEATS_PRO;
    }

}