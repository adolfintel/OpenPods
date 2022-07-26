package com.dosse.airpods.pods.data;

public class AirPods3 extends RegularPods {

    public AirPods3(Pod leftPod, Pod rightPod, Pod casePod) {
        super(leftPod, rightPod, casePod);
    }

    @Override
    public String getModel () {
        return MODEL_AIRPODS_GEN3;
    }

}