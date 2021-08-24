package com.dosse.airpods.utils;

import com.dosse.airpods.PodsService;

public class ScannerUtils {

    /*
     * Decodes the byte array to a hexadecimal string
     */
    public static String decodeHex (byte[] bArr) {
        StringBuilder ret = new StringBuilder();

        for (byte b : bArr)
            ret.append(String.format("%02X", b));

        return ret.toString();
    }

    public static boolean isFlipped (String str) {
        return (Integer.parseInt("" + str.charAt(10), 16) & 0x02) == 0;
    }

    /*
     * Check if model is airpods max
     */
    public static boolean isMax () {
        return PodsService.model.equals(PodsService.MODEL_AIRPODS_MAX);
    }

}
