package com.dosse.airpods.pods.models;

public interface IPods {

    String getModel ();

    boolean isSingle ();

    boolean isDisconnected ();

    String parseStatusForLogger ();

}