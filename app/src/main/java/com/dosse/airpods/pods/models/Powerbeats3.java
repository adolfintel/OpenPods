package com.dosse.airpods.pods.models;

import com.dosse.airpods.R;
import com.dosse.airpods.pods.Pod;

public class Powerbeats3 extends SinglePods {

    public Powerbeats3 (Pod singlePod) {
        super(singlePod);
    }

    @Override
    public int getDrawable () {
        return getPod().isConnected() ? R.drawable.powerbeats3 : R.drawable.powerbeats3_disconnected;
    }

    @Override
    public String getModel () {
        return Constants.MODEL_POWERBEATS_3;
    }

}