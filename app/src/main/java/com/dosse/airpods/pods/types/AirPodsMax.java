package com.dosse.airpods.pods.types;

import com.dosse.airpods.R;

public class AirPodsMax extends SinglePods {

    public AirPodsMax (Pod singlePod) {
        super(singlePod);
    }

    @Override
    public int getDrawable () {
        return getPod().isConnected() ? R.drawable.podmax : R.drawable.podmax_disconnected;
    }

    @Override
    public String getModel () {
        return MODEL_AIRPODS_MAX;
    }

}