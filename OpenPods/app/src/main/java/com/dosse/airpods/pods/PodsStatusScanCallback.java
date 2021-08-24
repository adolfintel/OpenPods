package com.dosse.airpods.pods;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.os.SystemClock;

import com.dosse.airpods.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * TODO: proper javadoc
 * On a normal OS, we would use the bluetooth address of the device to filter out beacons from other devices.
 * UNFORTUNATELY, someone at google was so concerned about privacy (yea, as if they give a shit) that he decided it was a good idea to not allow access to the bluetooth address of incoming BLE beacons.
 * As a result, we have no reliable way to make sure that the beacon comes from YOUR airpods and not the guy sitting next to you on the bus.
 * What we did to workaround this issue is this:
 * - When a beacon arrives that looks like a pair of AirPods, look at the other beacons received in the last 10 seconds and get the strongest one
 * - If the strongest beacon's fake address is the same as this, use this beacon; otherwise use the strongest beacon
 * - Filter for signals stronger than -60db
 * - Decode...
 */
public abstract class PodsStatusScanCallback extends ScanCallback {

    public static final long RECENT_BEACONS_MAX_T_NS = 10000000000L; //10s

    public static final int AIRPODS_MANUFACTURER = 76;
    public static final int AIRPODS_DATA_LENGTH = 27;
    public static final int MIN_RSSI = -60;

    private final List<ScanResult> recentBeacons = new ArrayList<>();

    public abstract void onStatus (PodsStatus status);

    public static List<ScanFilter> getScanFilters () {
        byte[] manufacturerData = new byte[AIRPODS_DATA_LENGTH];
        byte[] manufacturerDataMask = new byte[AIRPODS_DATA_LENGTH];

        manufacturerData[0] = 7;
        manufacturerData[1] = 25;

        manufacturerDataMask[0] = -1;
        manufacturerDataMask[1] = -1;

        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setManufacturerData(AIRPODS_MANUFACTURER, manufacturerData, manufacturerDataMask);

        return Collections.singletonList(builder.build());
    }

    @Override
    public void onBatchScanResults (List<ScanResult> scanResults) {
        for (ScanResult result : scanResults)
            onScanResult(-1, result);

        super.onBatchScanResults(scanResults);
    }

    @Override
    public void onScanResult (int callbackType, ScanResult result) {
        try {
            if (!isAirpodsResult(result))
                return;

            result.getDevice().getAddress();

            Logger.debug(result.getRssi() + "db");
            Logger.debug(decodeResult(result));

            result = getBestResult(result);
            if (result == null || result.getRssi() < MIN_RSSI)
                return;

            PodsStatus status = new PodsStatus(decodeResult(result));
            onStatus(status);
        } catch (Throwable t) {
            Logger.error(t);
        }
    }

    private ScanResult getBestResult (ScanResult result) {
        recentBeacons.add(result);
        ScanResult strongestBeacon = null;

        for (int i = 0; i < recentBeacons.size(); i++) {
            if (SystemClock.elapsedRealtimeNanos() - recentBeacons.get(i).getTimestampNanos() > RECENT_BEACONS_MAX_T_NS) {
                recentBeacons.remove(i--);
                continue;
            }

            if (strongestBeacon == null || strongestBeacon.getRssi() < recentBeacons.get(i).getRssi())
                strongestBeacon = recentBeacons.get(i);
        }

        if (strongestBeacon != null && Objects.equals(strongestBeacon.getDevice().getAddress(), result.getDevice().getAddress()))
            strongestBeacon = result;

        return strongestBeacon;
    }

    private static boolean isAirpodsResult (ScanResult result) {
        return result != null && result.getScanRecord() != null && isDataValid(result.getScanRecord().getManufacturerSpecificData(AIRPODS_MANUFACTURER));
    }

    private static boolean isDataValid (byte[] data) {
        return data != null && data.length == AIRPODS_DATA_LENGTH;
    }

    private static String decodeResult (ScanResult result) {
        if (result != null && result.getScanRecord() != null) {
            byte[] data = result.getScanRecord().getManufacturerSpecificData(AIRPODS_MANUFACTURER);
            if (isDataValid(data))
                return decodeHex(data);
        }
        return null;
    }

    public static String decodeHex (byte[] bArr) {
        StringBuilder ret = new StringBuilder();

        for (byte b : bArr)
            ret.append(String.format("%02X", b));

        return ret.toString();
    }

}