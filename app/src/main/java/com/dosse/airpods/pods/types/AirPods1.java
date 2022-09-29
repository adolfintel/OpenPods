package com.dosse.airpods.pods.types;

public class AirPods1 extends RegularPods {

    public AirPods1 (Pod leftPod, Pod rightPod, Pod casePod) {
        super(leftPod, rightPod, casePod);
    }

    @Override
    public String getModel () {
        return MODEL_AIRPODS_GEN1;
    }

}