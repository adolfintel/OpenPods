package com.dosse.airpods.pods.models;

import com.dosse.airpods.pods.Pod;

public class AirPods3 extends RegularPods {

    public AirPods3(Pod leftPod, Pod rightPod, Pod casePod) {
        super(leftPod, rightPod, casePod);
    }

    @Override
    public String getModel () {
        return Constants.MODEL_AIRPODS_GEN3;
    }

}