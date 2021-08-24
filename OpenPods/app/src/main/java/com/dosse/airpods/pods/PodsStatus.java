package com.dosse.airpods.pods;

import java.util.Locale;

/**
 * Decoding the beacon:
 * This was done through reverse engineering. Hopefully it's correct.
 * - The beacon coming from a pair of AirPods contains a manufacturer specific data field n°76 of 27 bytes
 * - We convert this data to a hexadecimal string
 * - The 12th and 13th characters in the string represent the charge of the left and right pods.
 * Under unknown circumstances[1], they are right and left instead (see isFlipped). Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
 * - The 15th character in the string represents the charge of the case. Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
 * - The 14th character in the string represents the "in charge" status.
 * Bit 0 (LSB) is the left pod; Bit 1 is the right pod; Bit 2 is the case. Bit 3 might be case open/closed but I'm not sure and it's not used
 * - The 11th character in the string represents the in-ear detection status. Bit 1 is the left pod; Bit 3 is the right pod.
 * - The 7th character in the string represents the AirPods model (E=AirPods pro, A=AirPods max)
 *
 * Notes:
 * 1) - isFlipped set by bit 1 of 10th character in the string; seems to be related to in-ear detection;
 */
public class PodsStatus {

    public static final String MODEL_AIRPODS_NORMAL = "airpods12", MODEL_AIRPODS_PRO = "airpodspro", MODEL_AIRPODS_MAX = "airpodsmax";

    public static final PodsStatus DISCONNECTED = new PodsStatus();

    private Pod leftPod, rightPod, casePod, maxPod;
    private String model = MODEL_AIRPODS_NORMAL;
    private final long timestamp = System.currentTimeMillis();

    public PodsStatus () {
    }

    public PodsStatus (String status) {
        if (status == null)
            return;

        boolean flip = isFlipped(status);

        int leftStatus = Integer.parseInt("" + status.charAt(flip ? 12 : 13), 16); // Left airpod (0-10 batt; 15=disconnected)
        int rightStatus = Integer.parseInt("" + status.charAt(flip ? 13 : 12), 16); // Right airpod (0-10 batt; 15=disconnected)
        int caseStatus = Integer.parseInt("" + status.charAt(15), 16); // Case (0-10 batt; 15=disconnected)
        int maxStatus = Integer.parseInt("" + status.charAt(13), 16); // Airpods max (0-10 batt; 15=disconnected)

        int chargeStatus = Integer.parseInt("" + status.charAt(14), 16); // Charge status (bit 0=left; bit 1=right; bit 2=case)

        boolean chargeL = (chargeStatus & (flip ? 0b00000010 : 0b00000001)) != 0;
        boolean chargeR = (chargeStatus & (flip ? 0b00000001 : 0b00000010)) != 0;
        boolean chargeCase = (chargeStatus & 0b00000100) != 0;
        boolean chargeMax = (chargeStatus & 0b00000001) != 0;

        int inEarStatus = Integer.parseInt("" + status.charAt(11), 16); // InEar status (bit 1=left; bit 3=right)

        boolean inEarL = (inEarStatus & (flip ? 0b00001000 : 0b00000010)) != 0;
        boolean inEarR = (inEarStatus & (flip ? 0b00000010 : 0b00001000)) != 0;

        switch (status.charAt(7)) { // Detect if these are AirPods Pro/Max or regular ones
            case 'E': model = MODEL_AIRPODS_PRO;
                break;
            case 'A': model = MODEL_AIRPODS_MAX;
                break;
            default: model = MODEL_AIRPODS_NORMAL;
        }

        leftPod = new Pod(leftStatus, chargeL, inEarL);
        rightPod = new Pod(rightStatus, chargeR, inEarR);
        casePod = new Pod(caseStatus, chargeCase, false);
        maxPod = new Pod(maxStatus, chargeMax, false);
    }

    public static boolean isFlipped (String str) {
        return (Integer.parseInt("" + str.charAt(10), 16) & 0x02) == 0;
    }

    //region Pod
    public Pod getLeftPod () {
        return leftPod;
    }

    public Pod getRightPod () {
        return rightPod;
    }

    public Pod getCasePod () {
        return casePod;
    }

    public Pod getMaxPod () {
        return maxPod;
    }
    //endregion

    public String parseStatusForLogger () {
        return isAirpodsMax() ?
                String.format(Locale.getDefault(), "Battery: %d%s Model: %s",
                        maxPod.getStatus(), maxPod.isCharging() ? "+" : "", model) :
                String.format(Locale.getDefault(), "Left: %d%s%s Right: %d%s%s Case: %d%s Model: %s",
                        leftPod.getStatus(), leftPod.isCharging() ? "+" : "", leftPod.isInEar() ? "$" : "",
                        rightPod.getStatus(), rightPod.isCharging() ? "+" : "", rightPod.isInEar() ? "$" : "",
                        casePod.getStatus(), casePod.isCharging() ? "+" : "", model);
    }

    public boolean isAllDisconnected () {
        if (this == DISCONNECTED)
            return true;

        return leftPod.isDisconnected() &&
                rightPod.isDisconnected() &&
                casePod.isDisconnected() &&
                maxPod.isDisconnected();
    }

    //region Model
    public boolean isAirpods () {
        return model.equals(MODEL_AIRPODS_NORMAL);
    }

    public boolean isAirpodsPro () {
        return model.equals(MODEL_AIRPODS_PRO);
    }

    public boolean isAirpodsMax () {
        return model.equals(MODEL_AIRPODS_MAX);
    }
    //endregion

    public long getTimestamp () {
        return timestamp;
    }

}