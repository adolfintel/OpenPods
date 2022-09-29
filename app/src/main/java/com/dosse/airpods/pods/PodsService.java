package com.dosse.airpods.pods;

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
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.provider.Settings;
import androidx.annotation.RequiresApi;

import com.dosse.airpods.R;
import com.dosse.airpods.notification.NotificationThread;
import com.dosse.airpods.receivers.BluetoothListener;
import com.dosse.airpods.receivers.BluetoothReceiver;
import com.dosse.airpods.receivers.ScreenReceiver;
import com.dosse.airpods.utils.Logger;

import java.util.Objects;

import static com.dosse.airpods.pods.PodsStatusScanCallback.getScanFilters;
import static com.dosse.airpods.utils.SharedPreferencesUtils.isSavingBattery;

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them (easier said than done thanks to google's autism)
 * - Display the notification with the status
 */
public class PodsService extends Service {

    private BluetoothLeScanner btScanner;
    private PodsStatus status = PodsStatus.DISCONNECTED;

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
    @SuppressLint("MissingPermission")
    private void startAirPodsScanner () {
        try {
            Logger.debug("START SCANNER");

            BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter btAdapter = btManager.getAdapter();

            if (btAdapter == null) {
                Logger.debug("No BT");
                return;
            }

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
                    .setScanMode(isSavingBattery(getApplicationContext()) ? ScanSettings.SCAN_MODE_LOW_POWER : ScanSettings.SCAN_MODE_LOW_LATENCY)
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

    @SuppressLint("MissingPermission")
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForeground(101, createBackgroundNotification());

        unregisterBtReceiver();

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
        unregisterScreenReceiver();

        if (isSavingBattery(getApplicationContext())) {
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

    @SuppressLint("MissingPermission")
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
        unregisterBtReceiver();
        unregisterScreenReceiver();
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

    // Foreground service for background notification (confusing I know).
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

    private void unregisterBtReceiver () {
        try {
            if (btReceiver != null) {
                unregisterReceiver(btReceiver);
                btReceiver = null;
            }
        } catch (Throwable t) {
            Logger.error(t);
        }
    }

    private void unregisterScreenReceiver () {
        try {
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver);
                screenReceiver = null;
            }
        } catch (Throwable t) {
            Logger.error(t);
        }
    }

}
