package com.dosse.airpods.pods.types;

import com.dosse.airpods.R;

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
        return MODEL_BEATS_STUDIO_3;
    }

}