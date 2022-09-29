package com.dosse.airpods.pods.types;

import com.dosse.airpods.R;

public class BeatsFlex extends SinglePods {

    public BeatsFlex (Pod singlePod) {
        super(singlePod);
    }

    @Override
    public int getDrawable () {
        return getPod().isConnected() ? R.drawable.beatsflex : R.drawable.beatsflex_disconnected;
    }

    @Override
    public String getModel () {
        return MODEL_BEATS_FLEX;
    }

}