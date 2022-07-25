package com.dosse.airpods.pods.data;

public interface IPods {

    String MODEL_AIRPODS_GEN1 = "airpods1";
    String MODEL_AIRPODS_GEN2 = "airpods2";
    String MODEL_AIRPODS_GEN3 = "airpods3";
    String MODEL_AIRPODS_PRO = "airpodspro";
    String MODEL_AIRPODS_MAX = "airpodsmax";
    String MODEL_BEATS_X = "beatsx";
    String MODEL_BEATS_FLEX = "beatsflex";
    String MODEL_BEATS_SOLO_3 = "beatssolo3";
    String MODEL_BEATS_STUDIO_3 = "beatsstudio3";
    String MODEL_POWERBEATS_3 = "powerbeats3";
    String MODEL_POWERBEATS_PRO = "powerbeatspro";
    String MODEL_UNKNOWN = "unknown";


    String getModel ();

    boolean isSingle ();

    boolean isDisconnected ();

    String parseStatusForLogger ();

}