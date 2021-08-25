package com.dosse.airpods.pods.data;

import com.dosse.airpods.R;

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
        return MODEL_BEATS_SOLO_3;
    }

}