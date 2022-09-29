package com.dosse.airpods.pods.models;

import com.dosse.airpods.R;
import com.dosse.airpods.pods.Pod;

public class BeatsStudio3 extends SinglePods {

    public BeatsStudio3 (Pod singlePod) {
        super(singlePod);
    }

    @Override
    public int getDrawable () {
        return getPod().isConnected() ? R.drawable.beatsstudio3 : R.drawable.beatsstudio3_disconnected;
    }

    @Override
    public String getModel () {
        return Constants.MODEL_BEATS_STUDIO_3;
    }

}