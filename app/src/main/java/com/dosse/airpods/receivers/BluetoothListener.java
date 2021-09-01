package com.dosse.airpods.receivers;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

public abstract class BluetoothListener implements BluetoothProfile.ServiceListener {

    public abstract boolean onConnect (BluetoothDevice bluetoothDevice);

    public abstract void onDisconnect ();

    @Override
    public void onServiceConnected (int profile, BluetoothProfile bluetoothProfile) {
        if (profile == BluetoothProfile.HEADSET)
            for (BluetoothDevice device : bluetoothProfile.getConnectedDevices())
                if (onConnect(device))
                    break;
    }

    @Override
    public void onServiceDisconnected (int profile) {
        if (profile == BluetoothProfile.HEADSET)
            onDisconnect();
    }

}