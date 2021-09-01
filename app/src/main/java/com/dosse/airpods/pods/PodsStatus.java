package com.dosse.airpods.pods;

import com.dosse.airpods.pods.data.AirPods1;
import com.dosse.airpods.pods.data.AirPods2;
import com.dosse.airpods.pods.data.AirPodsMax;
import com.dosse.airpods.pods.data.AirPodsPro;
import com.dosse.airpods.pods.data.BeatsFlex;
import com.dosse.airpods.pods.data.BeatsSolo3;
import com.dosse.airpods.pods.data.BeatsStudio3;
import com.dosse.airpods.pods.data.BeatsX;
import com.dosse.airpods.pods.data.IPods;
import com.dosse.airpods.pods.data.Pod;
import com.dosse.airpods.pods.data.Powerbeats3;
import com.dosse.airpods.pods.data.PowerbeatsPro;
import com.dosse.airpods.pods.data.RegularPods;

/**
 * Decoding the beacon:
 * This was done through reverse engineering. Hopefully it's correct.
 * - The beacon coming from a pair of AirPods/Beats contains a manufacturer specific data field n°76 of 27 bytes
 * - We convert this data to a hexadecimal string
 * - The 12th and 13th characters in the string represent the charge of the left and right pods.
 * Under unknown circumstances[1], they are right and left instead (see isFlipped). Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
 * - The 15th character in the string represents the charge of the case. Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
 * - The 14th character in the string represents the "in charge" status.
 * Bit 0 (LSB) is the left pod; Bit 1 is the right pod; Bit 2 is the case. Bit 3 might be case open/closed but I'm not sure and it's not used
 * - The 11th character in the string represents the in-ear detection status. Bit 1 is the left pod; Bit 3 is the right pod.
 * - The 7th character in the string represents the model
 *
 * Notes:
 * 1) - isFlipped set by bit 1 of 10th character in the string; seems to be related to in-ear detection;
 */
public class PodsStatus {

    public static final PodsStatus DISCONNECTED = new PodsStatus();

    private IPods pods;
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
        int singleStatus = Integer.parseInt("" + status.charAt(13), 16); // Single (0-10 batt; 15=disconnected)

        int chargeStatus = Integer.parseInt("" + status.charAt(14), 16); // Charge status (bit 0=left; bit 1=right; bit 2=case)

        boolean chargeL = (chargeStatus & (flip ? 0b00000010 : 0b00000001)) != 0;
        boolean chargeR = (chargeStatus & (flip ? 0b00000001 : 0b00000010)) != 0;
        boolean chargeCase = (chargeStatus & 0b00000100) != 0;
        boolean chargeSingle = (chargeStatus & 0b00000001) != 0;

        int inEarStatus = Integer.parseInt("" + status.charAt(11), 16); // InEar status (bit 1=left; bit 3=right)

        boolean inEarL = (inEarStatus & (flip ? 0b00001000 : 0b00000010)) != 0;
        boolean inEarR = (inEarStatus & (flip ? 0b00000010 : 0b00001000)) != 0;

        Pod leftPod = new Pod(leftStatus, chargeL, inEarL);
        Pod rightPod = new Pod(rightStatus, chargeR, inEarR);
        Pod casePod = new Pod(caseStatus, chargeCase, false);
        Pod singlePod = new Pod(singleStatus, chargeSingle, false);

        // Detect which model
        switch (status.charAt(7)) {
            case '2': pods = new AirPods1(leftPod, rightPod, casePod); // Airpods 1st gen
                break;
            case 'F': pods = new AirPods2(leftPod, rightPod, casePod); // Airpods 2nd gen
                break;
            case 'E': pods = new AirPodsPro(leftPod, rightPod, casePod); // Airpods Pro
                break;
            case 'A': pods = new AirPodsMax(singlePod); // Airpods Max
                break;
            case 'B': pods = new PowerbeatsPro(leftPod, rightPod, casePod); // Powerbeats Pro
                break;
            case '5': pods = new BeatsX(singlePod); // Beats X
                break;
            case '0': pods = new BeatsFlex(singlePod); // Beats Flex
                break;
            case '6': pods = new BeatsSolo3(singlePod); // Beats Solo 3
                break;
            case '9': pods = new BeatsStudio3(singlePod); // Beats Studio 3
                break;
            case '3': pods = new Powerbeats3(singlePod); // Powerbeats 3
                break;
            default: pods = new RegularPods(leftPod, rightPod, casePod); // Unknown
        }
    }

    public static boolean isFlipped (String str) {
        return (Integer.parseInt("" + str.charAt(10), 16) & 0x02) == 0;
    }

    public IPods getAirpods () {
        return pods;
    }

    public boolean isAllDisconnected () {
        if (this == DISCONNECTED)
            return true;

        return pods.isDisconnected();
    }

    public long getTimestamp () {
        return timestamp;
    }

}