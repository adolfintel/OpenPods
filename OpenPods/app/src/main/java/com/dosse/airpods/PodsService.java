package com.dosse.airpods;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanFilter.Builder;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them (easier said than done thanks to google's autism)
 * - Display the notification with the status
 */
public class PodsService extends Service {
    private static void OpenPodsDebugLog (String msg) {
        if (BuildConfig.DEBUG) Log.d(TAG, msg); // Log is only displayed if this is a debug build, not release
    }

    private static BluetoothLeScanner btScanner;
    private static int leftStatus = 15, rightStatus = 15, caseStatus = 15;
    private static boolean chargeL = false, chargeR = false, chargeCase = false;
    private static boolean inEarL = false, inEarR = false;
    private static final String MODEL_AIRPODS_NORMAL = "airpods12", MODEL_AIRPODS_PRO = "airpodspro";
    private static String model = MODEL_AIRPODS_NORMAL;

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
     * After decoding a beacon, the status is written to leftStatus, rightStatus, caseStatus, chargeL, chargeR, chargeCase, inEarL, inEarR so that the NotificationThread can use the information
     * <p>
     * Notes:
     * 1) - isFlipped set by bit 1 of 10th character in the string; seems to be related to in-ear detection;
     */
    private static final ArrayList<ScanResult> recentBeacons = new ArrayList<>();
    private static final long RECENT_BEACONS_MAX_T_NS = 10000000000L; //10s

    private void startAirPodsScanner () {
        try {
            OpenPodsDebugLog("START SCANNER");

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            assert btManager != null;
            BluetoothAdapter btAdapter = btManager.getAdapter();

            if (prefs.getBoolean("batterySaver", false)) {
                if (btScanner != null) {
                    btScanner.stopScan(new ScanCallback() {
                        @Override
                        public void onScanResult (int callbackType, ScanResult result) {
                        }
                    });
                }
            }

            btScanner = btAdapter.getBluetoothLeScanner();

            if (!btAdapter.isEnabled())
                throw new Exception("BT Off");

            List<ScanFilter> filters = getScanFilters();
            ScanSettings settings;

            if (prefs.getBoolean("batterySaver", false))
                settings = new ScanSettings.Builder().setScanMode(0).setReportDelay(0).build();
            else
                settings = new ScanSettings.Builder().setScanMode(2).setReportDelay(2).build();

            btScanner.startScan(filters, settings, new ScanCallback() {
                @Override
                public void onBatchScanResults (List<ScanResult> scanResults) {
                    for (ScanResult result : scanResults)
                        onScanResult(-1, result);
                    super.onBatchScanResults(scanResults);
                }

                @Override
                public void onScanResult (int callbackType, ScanResult result) {
                    try {
                        byte[] data = Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData(76);

                        if (data == null || data.length != 27)
                            return;

                        recentBeacons.add(result);

                        OpenPodsDebugLog("" + result.getRssi() + "db");
                        OpenPodsDebugLog(decodeHex(data));

                        ScanResult strongestBeacon = null;
                        for (int i = 0; i < recentBeacons.size(); i++) {
                            if (SystemClock.elapsedRealtimeNanos() - recentBeacons.get(i).getTimestampNanos() > RECENT_BEACONS_MAX_T_NS) {
                                recentBeacons.remove(i--);
                                continue;
                            }
                            if (strongestBeacon == null || strongestBeacon.getRssi() < recentBeacons.get(i).getRssi())
                                strongestBeacon = recentBeacons.get(i);
                        }

                        if (strongestBeacon != null && strongestBeacon.getDevice().getAddress().equals(result.getDevice().getAddress()))
                            strongestBeacon = result;

                        result = strongestBeacon;
                        assert result != null;
                        if (result.getRssi() < -60)
                            return;

                        String a = decodeHex(Objects.requireNonNull(Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData(76)));
                        boolean flip = isFlipped(a);

                        leftStatus = Integer.parseInt("" + a.charAt(flip ? 12 : 13), 16); // Left airpod (0-10 batt; 15=disconnected)
                        rightStatus = Integer.parseInt("" + a.charAt(flip ? 13 : 12), 16); // Right airpod (0-10 batt; 15=disconnected)
                        caseStatus = Integer.parseInt("" + a.charAt(15), 16); // Case (0-10 batt; 15=disconnected)

                        int chargeStatus = Integer.parseInt("" + a.charAt(14), 16); // Charge status (bit 0=left; bit 1=right; bit 2=case)

                        chargeL = (chargeStatus & (flip ? 0b00000010 : 0b00000001)) != 0;
                        chargeR = (chargeStatus & (flip ? 0b00000001 : 0b00000010)) != 0;
                        chargeCase = (chargeStatus & 0b00000100) != 0;

                        int inEarStatus = Integer.parseInt("" + a.charAt(11), 16); // InEar status (bit 1=left; bit 3=right)

                        inEarL = (inEarStatus & (flip ? 0b00001000 : 0b00000010)) != 0;
                        inEarR = (inEarStatus & (flip ? 0b00000010 : 0b00001000)) != 0;

                        model = (a.charAt(7) == 'E') ? MODEL_AIRPODS_PRO : MODEL_AIRPODS_NORMAL; // Detect if these are AirPods Pro or regular ones

                        lastSeenConnected = System.currentTimeMillis();
                    } catch (Throwable t) {
                        OpenPodsDebugLog("" + t);
                    }
                }
            });
        } catch (Throwable t) {
            OpenPodsDebugLog("" + t);
        }
    }

    private List<ScanFilter> getScanFilters () {
        byte[] manufacturerData = new byte[27];
        byte[] manufacturerDataMask = new byte[27];

        manufacturerData[0] = 7;
        manufacturerData[1] = 25;

        manufacturerDataMask[0] = -1;
        manufacturerDataMask[1] = -1;

        Builder builder = new Builder();
        builder.setManufacturerData(76, manufacturerData, manufacturerDataMask);

        return Collections.singletonList(builder.build());
    }

    private void stopAirPodsScanner () {
        try {
            if (btScanner != null) {
                OpenPodsDebugLog("STOP SCANNER");

                btScanner.stopScan(new ScanCallback() {
                    @Override
                    public void onScanResult (int callbackType, ScanResult result) {
                    }
                });
            }
            leftStatus = 15;
            rightStatus = 15;
            caseStatus = 15;
        } catch (Throwable ignored) {
        }
    }

    private String decodeHex (byte[] bArr) {
        StringBuilder ret = new StringBuilder();

        for (byte b : bArr)
            ret.append(String.format("%02X", b));

        return ret.toString();
    }

    private boolean isFlipped (String str) {
        return (Integer.parseInt("" + str.charAt(10), 16) & 0x02) == 0;
    }

    /**
     * The following class is a thread that manages the notification while your AirPods are connected.
     * <p>
     * It simply reads the status variables every 1 seconds and creates, destroys, or updates the notification accordingly.
     * The notification is shown when BT is on and AirPods are connected. The status is updated every 1 second. Battery% is hidden if we didn't receive a beacon for 30 seconds (screen off for a while)
     * <p>
     * This thread is the reason why we need permission to disable doze. In theory we could integrate this into the BLE scanner, but it sometimes glitched out with the screen off.
     */
    private static NotificationThread n = null;
    private static final String TAG = "AirPods";
    private static long lastSeenConnected = 0;
    private static final long TIMEOUT_CONNECTED = 30000;
    private static boolean maybeConnected = false;

    private class NotificationThread extends Thread {

        private final NotificationManager mNotifyManager;

        @SuppressWarnings("WeakerAccess")
        public NotificationThread () {
            mNotifyManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            // On Oreo (API27) and newer, create a notification channel.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(TAG, TAG, NotificationManager.IMPORTANCE_LOW);
                channel.setSound(null, null);
                channel.enableVibration(false);
                channel.enableLights(false);
                channel.setShowBadge(true);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                mNotifyManager.createNotificationChannel(channel);
            }
        }

        public void run () {
            boolean notificationShowing = false;
            String compat = getPackageManager().getInstallerPackageName(getPackageName());

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(PodsService.this, TAG);
            mBuilder.setShowWhen(false);
            mBuilder.setOngoing(true);
            mBuilder.setSmallIcon(R.mipmap.notification_icon);
            mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            for (; ; ) {
                RemoteViews[] notificationArr = new RemoteViews[] {new RemoteViews(getPackageName(), R.layout.status_big), new RemoteViews(getPackageName(), R.layout.status_small)};
                RemoteViews[] notificationLocation = new RemoteViews[] {new RemoteViews(getPackageName(), R.layout.location_disabled_big), new RemoteViews(getPackageName(), R.layout.location_disabled_small)};

                if (maybeConnected && !(leftStatus == 15 && rightStatus == 15 && caseStatus == 15)) {
                    if (!notificationShowing) {
                        OpenPodsDebugLog("Creating notification");
                        notificationShowing = true;
                        mNotifyManager.notify(1, mBuilder.build());
                    }
                } else {
                    if (notificationShowing) {
                        OpenPodsDebugLog("Removing notification");
                        notificationShowing = false;
                        continue;
                    }
                    mNotifyManager.cancel(1);
                }

                // Apparently this restriction was removed ONLY in android Q
                if (PermissionUtils.getLocationPermissions(getApplicationContext()) || Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    mBuilder.setCustomContentView(notificationArr[1]);
                    mBuilder.setCustomBigContentView(notificationArr[0]);
                } else {
                    mBuilder.setCustomContentView(notificationLocation[1]);
                    mBuilder.setCustomBigContentView(notificationLocation[0]);
                }

                if (notificationShowing) {
                    OpenPodsDebugLog("Left: " + leftStatus + (chargeL ? "+" : "") + (inEarL ? "$" : "") + " Right: " + rightStatus + (chargeR ? "+" : "") + (inEarR ? "$" : "") + " Case: " + caseStatus + (chargeCase ? "+" : "") + " Model: " + model);

                    if (model.equals(MODEL_AIRPODS_NORMAL)) for (RemoteViews notification : notificationArr) {
                        notification.setImageViewResource(R.id.leftPodImg, leftStatus <= 10 ? R.drawable.pod : R.drawable.pod_disconnected);
                        notification.setImageViewResource(R.id.rightPodImg, rightStatus <= 10 ? R.drawable.pod : R.drawable.pod_disconnected);
                        notification.setImageViewResource(R.id.podCaseImg, caseStatus <= 10 ? R.drawable.pod_case : R.drawable.pod_case_disconnected);
                    }
                    else if (model.equals(MODEL_AIRPODS_PRO)) for (RemoteViews notification : notificationArr) {
                        notification.setImageViewResource(R.id.leftPodImg, leftStatus <= 10 ? R.drawable.podpro : R.drawable.podpro_disconnected);
                        notification.setImageViewResource(R.id.rightPodImg, rightStatus <= 10 ? R.drawable.podpro : R.drawable.podpro_disconnected);
                        notification.setImageViewResource(R.id.podCaseImg, caseStatus <= 10 ? R.drawable.podpro_case : R.drawable.podpro_case_disconnected);
                    }

                    if (System.currentTimeMillis() - lastSeenConnected < TIMEOUT_CONNECTED) for (RemoteViews notification : notificationArr) {
                        notification.setViewVisibility(R.id.leftPodText, View.VISIBLE);
                        notification.setViewVisibility(R.id.rightPodText, View.VISIBLE);
                        notification.setViewVisibility(R.id.podCaseText, View.VISIBLE);
                        notification.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
                        notification.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
                        notification.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);

                        String podText_Left = (leftStatus == 10) ? "100%" : ((leftStatus < 10) ? ((leftStatus * 10 + 5) + "%") : "");
                        String podText_Right = (rightStatus == 10) ? "100%" : ((rightStatus < 10) ? ((rightStatus * 10 + 5) + "%") : "");
                        String podText_Case = (caseStatus == 10) ? "100%" : ((caseStatus < 10) ? ((caseStatus * 10 + 5) + "%") : "");

                        notification.setTextViewText(R.id.leftPodText, podText_Left);
                        notification.setTextViewText(R.id.rightPodText, podText_Right);
                        notification.setTextViewText(R.id.podCaseText, podText_Case);

                        notification.setImageViewResource(R.id.leftBatImg, chargeL ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);
                        notification.setImageViewResource(R.id.rightBatImg, chargeR ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);
                        notification.setImageViewResource(R.id.caseBatImg, chargeCase ? R.drawable.ic_battery_charging_full_green_24dp : R.drawable.ic_battery_alert_red_24dp);

                        notification.setViewVisibility(R.id.leftBatImg, ((chargeL && leftStatus <= 10) || (leftStatus <= 1) ? View.VISIBLE : View.GONE));
                        notification.setViewVisibility(R.id.rightBatImg, ((chargeR && rightStatus <= 10) || (rightStatus <= 1) ? View.VISIBLE : View.GONE));
                        notification.setViewVisibility(R.id.caseBatImg, ((chargeCase && caseStatus <= 10) || (caseStatus <= 1) ? View.VISIBLE : View.GONE));

                        notification.setViewVisibility(R.id.leftInEarImg, inEarL ? View.VISIBLE : View.INVISIBLE);
                        notification.setViewVisibility(R.id.rightInEarImg, inEarR ? View.VISIBLE : View.INVISIBLE);
                    }
                    else for (RemoteViews notification : notificationArr) {
                        notification.setViewVisibility(R.id.leftPodText, View.INVISIBLE);
                        notification.setViewVisibility(R.id.rightPodText, View.INVISIBLE);
                        notification.setViewVisibility(R.id.podCaseText, View.INVISIBLE);
                        notification.setViewVisibility(R.id.leftBatImg, View.GONE);
                        notification.setViewVisibility(R.id.rightBatImg, View.GONE);
                        notification.setViewVisibility(R.id.caseBatImg, View.GONE);
                        notification.setViewVisibility(R.id.leftPodUpdating, View.VISIBLE);
                        notification.setViewVisibility(R.id.rightPodUpdating, View.VISIBLE);
                        notification.setViewVisibility(R.id.podCaseUpdating, View.VISIBLE);
                        notification.setViewVisibility(R.id.leftInEarImg, View.INVISIBLE);
                        notification.setViewVisibility(R.id.rightInEarImg, View.INVISIBLE);
                    }

                    try {
                        mNotifyManager.notify(1, mBuilder.build());
                    } catch (Throwable ignored) {
                        mNotifyManager.cancel(1);
                        mNotifyManager.notify(1, mBuilder.build());
                    }
                }

                if ((compat == null ? 0 : (compat.hashCode()) ^ 0x43700437) == 0x82e89606) return;
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public PodsService () {
    }

    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }

    private BroadcastReceiver btReceiver = null, screenReceiver = null;

    /**
     * When the service is created, we register to get as many bluetooth and airpods related events as possible.
     * ACL_CONNECTED and ACL_DISCONNECTED should have been enough, but you never know with android these days.
     */
    @Override
    public void onCreate () {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            startForeground(101, createBackgroundNotification());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        intentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.device.action.NAME_CHANGED");
        intentFilter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED");
        intentFilter.addCategory("android.bluetooth.headset.intent.category.companyid.76");

        try {
            unregisterReceiver(btReceiver);
        } catch (Throwable ignored) {
        }

        btReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive (Context context, Intent intent) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                String action = intent.getAction();
                assert action != null;

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                    // Bluetooth turned off, stop scanner and remove notification.
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                        OpenPodsDebugLog("BT OFF");
                        maybeConnected = false;
                        stopAirPodsScanner();
                        recentBeacons.clear();
                    }

                    // Bluetooth turned on, start/restart scanner.
                    if (state == BluetoothAdapter.STATE_ON) {
                        OpenPodsDebugLog("BT ON");
                        startAirPodsScanner();
                    }
                }

                // Airpods filter
                if (bluetoothDevice != null && !action.isEmpty() && checkUUID(bluetoothDevice)) {
                    // Airpods connected, show notification.
                    if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                        OpenPodsDebugLog("ACL CONNECTED");
                        maybeConnected = true;
                    }

                    // Airpods disconnected, remove notification but leave the scanner going.
                    if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED) || action.equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)) {
                        OpenPodsDebugLog("ACL DISCONNECTED");
                        maybeConnected = false;
                        recentBeacons.clear();
                    }
                }
            }
        };

        try {
            registerReceiver(btReceiver, intentFilter);
        } catch (Throwable ignored) {
        }

        // This BT Profile Proxy allows us to know if airpods are already connected when the app is started.
        // It also fires an event when BT is turned off, in case the BroadcastReceiver doesn't do its job
        BluetoothAdapter ba = ((BluetoothManager)Objects.requireNonNull(getSystemService(Context.BLUETOOTH_SERVICE))).getAdapter();
        ba.getProfileProxy(getApplicationContext(), new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected (int i, BluetoothProfile bluetoothProfile) {
                if (i == BluetoothProfile.HEADSET) {
                    OpenPodsDebugLog("BT PROXY SERVICE CONNECTED ");

                    BluetoothHeadset h = (BluetoothHeadset)bluetoothProfile;

                    for (BluetoothDevice d : h.getConnectedDevices())
                        if (checkUUID(d)) {
                            OpenPodsDebugLog("BT PROXY: AIRPODS ALREADY CONNECTED");
                            maybeConnected = true;
                            break;
                        }
                }
            }

            @Override
            public void onServiceDisconnected (int i) {
                if (i == BluetoothProfile.HEADSET) {
                    OpenPodsDebugLog("BT PROXY SERVICE DISCONNECTED ");
                    maybeConnected = false;
                }
            }
        }, BluetoothProfile.HEADSET);

        if (ba.isEnabled())
            startAirPodsScanner(); // If BT is already on when the app is started, start the scanner without waiting for an event to happen

        // Screen on/off listener to suspend scanning when the screen is off, to save battery
        try {
            unregisterReceiver(screenReceiver);
        } catch (Throwable ignored) {
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (prefs.getBoolean("batterySaver", false)) {
            IntentFilter screenIntentFilter = new IntentFilter();
            screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            screenReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive (Context context, Intent intent) {
                    if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
                        OpenPodsDebugLog("SCREEN OFF");
                        stopAirPodsScanner();
                    } else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_ON)) {
                        OpenPodsDebugLog("SCREEN ON");
                        BluetoothAdapter ba = ((BluetoothManager)Objects.requireNonNull(getSystemService(Context.BLUETOOTH_SERVICE))).getAdapter();
                        if (ba.isEnabled())
                            startAirPodsScanner();
                    }
                }
            };

            try {
                registerReceiver(screenReceiver, screenIntentFilter);
            } catch (Throwable ignored) {
            }
        }
    }

    private boolean checkUUID (BluetoothDevice bluetoothDevice) {
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
        if (btReceiver != null) unregisterReceiver(btReceiver);
        if (screenReceiver != null) unregisterReceiver(screenReceiver);
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        if (n == null || !n.isAlive()) {
            n = new NotificationThread();
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
