package com.dosse.airpods.pods.models;

import com.dosse.airpods.R;
import com.dosse.airpods.pods.Pod;

public class BeatsSolo3 extends SinglePods {

    public BeatsSolo3 (Pod singlePod) {
        super(singlePod);
    }

    @Override
    public int getDrawable () {
        return getPod().isConnected() ? R.drawable.beatssolo3 : R.drawable.beatssolo3_disconnected;
    }

    @Override
    public String getModel () {
        return Constants.MODEL_BEATS_SOLO_3;
    }

}