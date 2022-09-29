package com.dosse.airpods.pods.models;

import com.dosse.airpods.R;
import com.dosse.airpods.pods.Pod;

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
        return Constants.MODEL_AIRPODS_MAX;
    }

}