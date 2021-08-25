package com.dosse.airpods.pods.data;

public class AirPods2 extends RegularPods {

    public AirPods2 (Pod leftPod, Pod rightPod, Pod casePod) {
        super(leftPod, rightPod, casePod);
    }

    @Override
    public String getModel () {
        return MODEL_AIRPODS_GEN2;
    }

}