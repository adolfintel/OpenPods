package com.dosse.airpods.pods.types;

import com.dosse.airpods.R;

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
        return MODEL_POWERBEATS_3;
    }

}