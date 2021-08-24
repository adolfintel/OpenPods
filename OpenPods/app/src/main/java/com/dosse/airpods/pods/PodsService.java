package com.dosse.airpods.pods;

import static com.dosse.airpods.pods.PodsStatusScanCallback.getScanFilters;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import com.dosse.airpods.receivers.BluetoothListener;
import com.dosse.airpods.receivers.BluetoothReceiver;
import com.dosse.airpods.notification.NotificationThread;
import com.dosse.airpods.R;
import com.dosse.airpods.receivers.ScreenReceiver;
import com.dosse.airpods.utils.Logger;

import java.util.Objects;

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them (easier said than done thanks to google's autism)
 * - Display the notification with the status
 */
public class PodsService extends Service {

    /**
     * The following method (startAirPodsScanner) creates a bluetooth LE scanner.
     * This scanner receives all beacons from nearby BLE devices (not just your devices!) so we need to do 3 things:
     * - Check that the beacon comes from something that looks like a pair of AirPods
     * - Make sure that it is YOUR pair of AirPods
     * - Decode the beacon to get the status
     * <p>
     * On a normal OS, we would use the bluetooth address of the device to filter out beacons from other devices.
     * UNFORTUNATELY, someone at google was so concerned about privacy (yea, as if they give a shit) that he decided it was a good idea to not allow access to the bluetooth address of incoming BLE beacons.
     * As a result, we have no reliable way to make sure that the beacon comes from YOUR airpods and not the guy sitting next to you on the bus.
     * What we did to workaround this issue is this:
     * - When a beacon arrives that looks like a pair of AirPods, look at the other beacons received in the last 10 seconds and get the strongest one
     * - If the strongest beacon's fake address is the same as this, use this beacon; otherwise use the strongest beacon
     * - Filter for signals stronger than -60db
     * - Decode...
     * <p>
     * Decoding the beacon:
     * This was done through reverse engineering. Hopefully it's correct.
     * - The beacon coming from a pair of AirPods contains a manufacturer specific data field n°76 of 27 bytes
     * - We convert this data to a hexadecimal string
     * - The 12th and 13th characters in the string represent the charge of the left and right pods. Under unknown circumstances[1], they are right and left instead (see isFlipped). Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
     * - The 15th character in the string represents the charge of the case. Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
     * - The 14th character in the string represents the "in charge" status. Bit 0 (LSB) is the left pod; Bit 1 is the right pod; Bit 2 is the case. Bit 3 might be case open/closed but I'm not sure and it's not used
     * - The 11th character in the string represents the in-ear detection status. Bit 1 is the left pod; Bit 3 is the right pod.
     * - The 7th character in the string represents the AirPods model (E=AirPods pro)
     * <p>
     * After decoding a beacon, the status is written to leftStatus, rightStatus, caseStatus, maxStatus, chargeL, chargeR, chargeCase, inEarL, inEarR so that the NotificationThread can use the information
     * <p>
     * Notes:
     * 1) - isFlipped set by bit 1 of 10th character in the string; seems to be related to in-ear detection;
     */

    private BluetoothLeScanner btScanner;
    private PodsStatus status = PodsStatus.DISCONNECTED;

    @SuppressLint("StaticFieldLeak")
    private static NotificationThread n = null;
    private static boolean maybeConnected = false;

    private BroadcastReceiver btReceiver = null;
    private BroadcastReceiver screenReceiver = null;
    private PodsStatusScanCallback scanCallback = null;

    /**
     * The following method (startAirPodsScanner) creates a bluetooth LE scanner.
     * This scanner receives all beacons from nearby BLE devices (not just your devices!) so we need to do 3 things:
     * - Check that the beacon comes from something that looks like a pair of AirPods
     * - Make sure that it is YOUR pair of AirPods
     * - Decode the beacon to get the status
     *
     * After decoding a beacon, the status is written to PodsStatus so that the NotificationThread can use the information
     */
    private void startAirPodsScanner () {
        try {
            Logger.debug("START SCANNER");

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean batterySaver = prefs.getBoolean("batterySaver", false);
            BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter btAdapter = btManager.getAdapter();

            if (btAdapter == null) {
                Logger.debug("No BT");
                return;
            }

            //if (batterySaver && btScanner != null && scanCallback != null) {
            if (btScanner != null && scanCallback != null) {
                btScanner.stopScan(scanCallback);
                scanCallback = null;
            }

            if (!btAdapter.isEnabled()) {
                Logger.debug("BT Off");
                return;
            }

            btScanner = btAdapter.getBluetoothLeScanner();

            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(batterySaver ? ScanSettings.SCAN_MODE_LOW_POWER : ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(1) // DON'T USE 0
                    .build();

            scanCallback = new PodsStatusScanCallback() {
                @Override
                public void onStatus (PodsStatus newStatus) {
                    status = newStatus;
                }
            };

            btScanner.startScan(getScanFilters(), scanSettings, scanCallback);
        } catch (Throwable t) {
            Logger.error(t);
        }
    }

    private void stopAirPodsScanner () {
        try {
            if (btScanner != null && scanCallback != null) {
                Logger.debug("STOP SCANNER");

                btScanner.stopScan(scanCallback);
                scanCallback = null;
            }
            status = PodsStatus.DISCONNECTED;
        } catch (Throwable t) {
            Logger.error(t);
        }
    }

    public PodsService () {
    }

    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }

    /**
     * When the service is created, we register to get as many bluetooth and airpods related events as possible.
     * ACL_CONNECTED and ACL_DISCONNECTED should have been enough, but you never know with android these days.
     */
    @Override
    public void onCreate () {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            startForeground(101, createBackgroundNotification());

        try {
            if (btReceiver != null) {
                unregisterReceiver(btReceiver);
                btReceiver = null;
            }
        } catch (Throwable t) {
            Logger.error(t);
        }

        btReceiver = new BluetoothReceiver() {
            @Override
            public void onStart () {
                // Bluetooth turned on, start/restart scanner.
                Logger.debug("BT ON");
                startAirPodsScanner();
            }

            @Override
            public void onStop () {
                // Bluetooth turned off, stop scanner and remove notification.
                Logger.debug("BT OFF");
                maybeConnected = false;
                stopAirPodsScanner();
            }

            @Override
            public void onConnect (BluetoothDevice bluetoothDevice) {
                // Airpods filter
                if (checkUUID(bluetoothDevice)) {
                    // Airpods connected, show notification.
                    Logger.debug("ACL CONNECTED");
                    maybeConnected = true;
                }
            }

            @Override
            public void onDisconnect (BluetoothDevice bluetoothDevice) {
                // Airpods filter
                if (checkUUID(bluetoothDevice)) {
                    // Airpods disconnected, remove notification but leave the scanner going.
                    Logger.debug("ACL DISCONNECTED");
                    maybeConnected = false;
                }
            }
        };

        try {
            registerReceiver(btReceiver, BluetoothReceiver.buildFilter());
        } catch (Throwable t) {
            Logger.error(t);
        }

        // This BT Profile Proxy allows us to know if airpods are already connected when the app is started.
        // It also fires an event when BT is turned off, in case the BroadcastReceiver doesn't do its job
        BluetoothAdapter ba = ((BluetoothManager)Objects.requireNonNull(getSystemService(Context.BLUETOOTH_SERVICE))).getAdapter();
        ba.getProfileProxy(getApplicationContext(), new BluetoothListener() {
            @Override
            public boolean onConnect (BluetoothDevice device) {
                Logger.debug("BT PROXY SERVICE CONNECTED ");

                if (checkUUID(device)) {
                    Logger.debug("BT PROXY: AIRPODS ALREADY CONNECTED");
                    maybeConnected = true;
                    return true;
                }

                return false;
            }

            @Override
            public void onDisconnect () {
                Logger.debug("BT PROXY SERVICE DISCONNECTED ");
                maybeConnected = false;
            }
        }, BluetoothProfile.HEADSET);

        if (ba.isEnabled())
            startAirPodsScanner(); // If BT is already on when the app is started, start the scanner without waiting for an event to happen

        // Screen on/off listener to suspend scanning when the screen is off, to save battery
        try {
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver);
                screenReceiver = null;
            }
        } catch (Throwable t) {
            Logger.error(t);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (prefs.getBoolean("batterySaver", false)) {
            screenReceiver = new ScreenReceiver() {
                @Override
                public void onStart () {
                    Logger.debug("SCREEN ON");
                    startAirPodsScanner();
                }

                @Override
                public void onStop () {
                    Logger.debug("SCREEN OFF");
                    stopAirPodsScanner();
                }
            };

            try {
                registerReceiver(screenReceiver, ScreenReceiver.buildFilter());
            } catch (Throwable t) {
                Logger.error(t);
            }
        }
    }

    private static boolean checkUUID (BluetoothDevice bluetoothDevice) {
        ParcelUuid[] AIRPODS_UUIDS = {
                ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
                ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74")
        };
        ParcelUuid[] uuids = bluetoothDevice.getUuids();

        if (uuids == null)
            return false;

        for (ParcelUuid u : uuids)
            for (ParcelUuid v : AIRPODS_UUIDS)
                if (u.equals(v)) return true;

        return false;
    }

    @Override
    public void onDestroy () {
        super.onDestroy();

        try {
            if (btReceiver != null) {
                unregisterReceiver(btReceiver);
                btReceiver = null;
            }
        } catch (Throwable t) {
            Logger.error(t);
        }

        try {
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver);
                screenReceiver = null;
            }
        } catch (Throwable t) {
            Logger.error(t);
        }
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        if (n == null || !n.isAlive()) {
            n = new NotificationThread(this) {
                @Override
                public boolean isConnected () {
                    return maybeConnected;
                }

                @Override
                public PodsStatus getStatus () {
                    return status;
                }
            };
            n.start();
        }
        return START_STICKY;
    }

    // Foreground service background notification (confusing I know).
    // Only enabled for API30+
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification createBackgroundNotification () {
        final String notChannelID = "FOREGROUND_ID";

        NotificationManager notManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notChannel = new NotificationChannel(notChannelID, getString(R.string.bg_noti_channel), NotificationManager.IMPORTANCE_LOW);
        notChannel.setShowBadge(false);
        notManager.createNotificationChannel(notChannel);

        Intent notIntent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                .putExtra(Settings.EXTRA_CHANNEL_ID, notChannelID);

        PendingIntent notPendingIntent = PendingIntent.getActivity(this, 1110, notIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(this, notChannelID)
                .setSmallIcon(R.drawable.pod_case)
                .setContentTitle(getString(R.string.bg_noti_title))
                .setContentText(getString(R.string.bg_noti_text))
                .setContentIntent(notPendingIntent)
                .setOngoing(true);

        return builder.build();
    }

}
