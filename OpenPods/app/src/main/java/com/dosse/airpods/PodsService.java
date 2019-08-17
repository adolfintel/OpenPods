package com.dosse.airpods;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is the class that does most of the work. It has 3 functions:
 * - Detect when AirPods are detected
 * - Receive beacons from AirPods and decode them (easier said than done thanks do google's autism)
 * - Display the notification with the status
 *
 */
public class PodsService extends Service {
    private static final boolean ENABLE_LOGGING=BuildConfig.DEBUG; //Log is only displayed if this is a debug build, not release

    private static BluetoothLeScanner btScanner;
    private static int leftStatus=15, rightStatus=15, caseStatus=15;
    private static boolean chargeL=false, chargeR=false, chargeCase=false;

    /**
     * The following method (startAirPodsScanner) creates a bluetoth LE scanner.
     * This scanner receives all beacons from nearby BLE devices (not just your devices!) so we need to do 3 things:
     * - Check that the beacon comes from something that looks like a pair of AirPods
     * - Make sure that it is YOUR pair of AirPods
     * - Decode the beacon to get the status
     *
     * On a normal OS, we would use the bluetooth address of the device to filter out beacons from other devices.
     * UNFORTUNATELY, someone at google was so concerned about privacy (yea, as if they give a shit) that he decided it was a good idea to not allow access to the bluetooth address of incoming BLE beacons. As a result, we have no reliable way to make sure that the beacon comes from YOUR airpods and not the guy sitting next to you on the bus.
     * What we did to workaround this issue is this:
     * - When a beacon arrives that looks like a pair of AirPods, look at the other beacons received in the last 10 seconds and get the strongest one
     * - If the strongest beacon's fake address is the same as this, use this beacon; otherwise use the strongest beacon
     * - Filter for signals stronger than -60db
     * - Decode...
     *
     * Decoding the beacon:
     * This was done through reverse engineering. Hopefully it's correct.
     * - The beacon coming from a pair of AirPods contains a manufacturer specific data field n°76 of 27 bytes
     * - We convert this data to a hexadecimal string
     * - The 12th and 13th characters in the string represent the charge of the left and right pods. Under unknown circumstances, they are right and left instead (see isFlipped). Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
     * - The 15th character in the string represents the charge of the case. Values between 0 and 10 are battery 0-100%; Value 15 means it's disconnected
     * - The 14th character in the string represents the "in charge" status. Bit 0 (LSB) is the left pod; Bit 1 is the right pod; Bit 2 is the case. Bit 3 might be case open/closed but I'm not sure and it's not used
     *
     * After decoding a beacon, the status is written to leftStatus, rightStatus, caseStatus, chargeL, chargeR, chargeCase so that the NotificationThread can use the information
     *
     */
    private static ArrayList<ScanResult> recentBeacons=new ArrayList<>();
    private static final long RECENT_BEACONS_MAX_T_NS=10000000000L; //10s
    private void startAirPodsScanner() {
        try {
            BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter btAdapter = btManager.getAdapter();
            btScanner = btAdapter.getBluetoothLeScanner();
            if (btAdapter == null) throw new Exception("No BT");
            if (!btAdapter.isEnabled()) throw new Exception("BT Off");

            List<ScanFilter> filters = getScanFilters();
            ScanSettings settings = new ScanSettings.Builder().setScanMode(2).setReportDelay(2).build();

            btScanner.startScan(
                    filters,
                    settings,
                    new ScanCallback() {
                @Override
                public void onBatchScanResults(List<ScanResult> scanResults) {
                    for (ScanResult result : scanResults) onScanResult(-1, result);
                    super.onBatchScanResults(scanResults);
                }

                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    try {
                        byte[] data = result.getScanRecord().getManufacturerSpecificData(76);
                        if (data == null||data.length!=27) return;
                        recentBeacons.add(result);
                        if(ENABLE_LOGGING) Log.d(TAG,""+result.getRssi()+"db");
                        if(ENABLE_LOGGING) Log.d(TAG, decodeHex(data));
                        ScanResult strongestBeacon=null;
                        for(int i=0;i<recentBeacons.size();i++){
                            if(SystemClock.elapsedRealtimeNanos()-recentBeacons.get(i).getTimestampNanos()>RECENT_BEACONS_MAX_T_NS){
                                recentBeacons.remove(i--);
                                continue;
                            }
                            if(strongestBeacon==null||strongestBeacon.getRssi()<recentBeacons.get(i).getRssi()) strongestBeacon=recentBeacons.get(i);
                        }
                        if(strongestBeacon!=null&&strongestBeacon.getDevice().getAddress().equals(result.getDevice().getAddress())) strongestBeacon=result;
                        result=strongestBeacon;
                        if(result.getRssi()<-60) return;
                        String a=decodeHex(result.getScanRecord().getManufacturerSpecificData(76));
                        String str = ""; //left airpod (0-10 batt; 15=disconnected)
                        String str2 = ""; //right airpod (0-10 batt; 15=disconnected)
                        if (isFlipped(a)) {
                            str = "" + a.charAt(12);
                            str2 = "" + a.charAt(13);
                        } else {
                            str = "" + a.charAt(13);
                            str2 = "" + a.charAt(12);
                        }
                        String str3 = "" + a.charAt(15); //case (0-10 batt; 15=disconnected)
                        String str4 = "" + a.charAt(14); //charge status (bit 0=left; bit 1=right; bit 2=case)
                        leftStatus = Integer.parseInt(str, 16);
                        rightStatus = Integer.parseInt(str2, 16);
                        caseStatus = Integer.parseInt(str3, 16);
                        int chargeStatus = Integer.parseInt(str4, 16);
                        chargeL = (chargeStatus & 0b00000001) != 0;
                        chargeR = (chargeStatus & 0b00000010) != 0;
                        chargeCase = (chargeStatus & 0b00000100) != 0;
                        lastSeenConnected = System.currentTimeMillis();
                    } catch (Throwable t) {
                        if(ENABLE_LOGGING) Log.d(TAG, "" + t);
                    }
                }
            });
        } catch (Throwable t) {
            if(ENABLE_LOGGING) Log.d(TAG, "" + t);
        }
    }

    private List<ScanFilter> getScanFilters() {
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

    private void stopAirPodsScanner(){
        try{
            if(btScanner!=null){
                btScanner.stopScan(new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {}});
            }
            leftStatus=15; rightStatus=15; caseStatus=15;
        }catch (Throwable t){}
    }

    private final char[] hexCharset = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    private String decodeHex(byte[] bArr) {
        char[] ret = new char[bArr.length * 2];
        for (int i = 0; i < bArr.length; i++) {
            int b = bArr[i] & 0xFF;
            ret[i*2] = hexCharset[b >>> 4];
            ret[i*2+1] = hexCharset[b & 0x0F];
        }
        return new String(ret);
    }

    private boolean isFlipped(String str) {
        return (Integer.toString(Integer.parseInt(""+str.charAt(10),16)+0x10,2)).charAt(3)=='0';
    }

    /**
     * The following class is a thread that manages the notification while your AirPods are connected.
     *
     * It simply reads the status variables every 1 seconds and creates, destroys, or updates the notification accordingly.
     * The notification is shown when BT is on and AirPods are connected. The status is updated every 1 second. Battery% is hidden if we didn't receive a beacon for 30 seconds (screen off for a while)
     *
     * This thread is the reason why we need permission to disable doze. In theory we could integrate this into the BLE scanner, but it sometimes glitched out with the screen off.
     *
     */
    private static NotificationThread n=null;
    private static final String TAG="AirPods";
    private static long lastSeenConnected=0;
    private static final long TIMEOUT_CONNECTED=30000;
    private static boolean maybeConnected =false;
    private class NotificationThread extends Thread{
        private boolean isLocationEnabled(){
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P){
                LocationManager service = (LocationManager) getSystemService(getApplicationContext().LOCATION_SERVICE);
                return service!=null&&service.isLocationEnabled();
            }else{
                try {
                    return Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF;
                }catch(Throwable t){
                    return true;
                }
            }
        }
        private NotificationManager mNotifyManager;
        public NotificationThread(){
            mNotifyManager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //on oreo and newer, create a notification channel
                NotificationChannel channel = new NotificationChannel(TAG, TAG, NotificationManager.IMPORTANCE_LOW);
                channel.enableVibration(false);
                channel.enableLights(false);
                channel.setShowBadge(true);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                mNotifyManager.createNotificationChannel(channel);
            }
        }
        public void run(){
            boolean notificationShowing=false;
            RemoteViews notificationBig=new RemoteViews(getPackageName(),R.layout.status_big);
            RemoteViews notificationSmall=new RemoteViews(getPackageName(),R.layout.status_small);
            RemoteViews locationDisabledBig=new RemoteViews(getPackageName(),R.layout.location_disabled_big);
            RemoteViews locationDisabledSmall=new RemoteViews(getPackageName(),R.layout.location_disabled_small);
            NotificationCompat.Builder mBuilder=new NotificationCompat.Builder(PodsService.this,TAG);
            mBuilder.setShowWhen(false);
            mBuilder.setOngoing(true);
            mBuilder.setSmallIcon(R.mipmap.notification_icon);
            for(;;){
                if(maybeConnected &&!(leftStatus==15&&rightStatus==15&&caseStatus==15)/*&&System.currentTimeMillis()-lastSeenConnected<TIMEOUT_CONNECTED*/){
                    if(!notificationShowing){
                        if(ENABLE_LOGGING) Log.d(TAG,"Creating notification");
                        notificationShowing=true;
                        mNotifyManager.notify(1,mBuilder.build());
                    }
                }else{
                    if(notificationShowing){
                        if(ENABLE_LOGGING) Log.d(TAG,"Removing notification");
                        notificationShowing=false;
                        continue;
                    }
                    mNotifyManager.cancel(1);
                }
                if(isLocationEnabled()) {
                    mBuilder.setCustomContentView(notificationSmall);
                    mBuilder.setCustomBigContentView(notificationBig);
                }else{
                    mBuilder.setCustomContentView(locationDisabledSmall);
                    mBuilder.setCustomBigContentView(locationDisabledBig);
                }
                if(notificationShowing){
                    if(ENABLE_LOGGING) Log.d(TAG,"Left: "+leftStatus+(chargeL?"+":"")+" "+"Right: "+rightStatus+(chargeR?"+":"")+" "+"Case: "+caseStatus+(chargeCase?"+":""));
                    notificationBig.setImageViewResource(R.id.leftPodImg,leftStatus<=10?R.drawable.left_pod:R.drawable.left_pod_disconnected);
                    notificationBig.setImageViewResource(R.id.rightPodImg,rightStatus<=10?R.drawable.right_pod:R.drawable.right_pod_disconnected);
                    notificationBig.setImageViewResource(R.id.podCaseImg,caseStatus<=10?R.drawable.pod_case:R.drawable.pod_case_disconnected);
                    if(System.currentTimeMillis()-lastSeenConnected<TIMEOUT_CONNECTED) {
                        notificationBig.setViewVisibility(R.id.leftPodText, View.VISIBLE);
                        notificationBig.setViewVisibility(R.id.rightPodText, View.VISIBLE);
                        notificationBig.setViewVisibility(R.id.podCaseText, View.VISIBLE);
                        notificationBig.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
                        notificationBig.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
                        notificationBig.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);
                        notificationSmall.setViewVisibility(R.id.leftPodText, View.VISIBLE);
                        notificationSmall.setViewVisibility(R.id.rightPodText, View.VISIBLE);
                        notificationSmall.setViewVisibility(R.id.podCaseText, View.VISIBLE);
                        notificationSmall.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
                        notificationSmall.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
                        notificationSmall.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);
                        notificationBig.setTextViewText(R.id.leftPodText, (leftStatus==10?"100%":leftStatus<10?((leftStatus+1)*10+"%"):"") + ((chargeL && leftStatus < 10) ? "+" : ""));
                        notificationBig.setTextViewText(R.id.rightPodText, (rightStatus==10?"100%":rightStatus<10?((rightStatus+1)*10+"%"):"") + ((chargeR && rightStatus < 10) ? "+" : ""));
                        notificationBig.setTextViewText(R.id.podCaseText, (caseStatus==10?"100%":caseStatus<10?((caseStatus+1)*10+"%"):"") + ((chargeCase && caseStatus < 10) ? "+" : ""));
                        notificationSmall.setTextViewText(R.id.leftPodText, (leftStatus==10?"100%":leftStatus<10?((leftStatus+1)*10+"%"):"") + ((chargeL && leftStatus < 10) ? "+" : ""));
                        notificationSmall.setTextViewText(R.id.rightPodText, (rightStatus==10?"100%":rightStatus<10?((rightStatus+1)*10+"%"):"") + ((chargeR && rightStatus < 10) ? "+" : ""));
                        notificationSmall.setTextViewText(R.id.podCaseText, (caseStatus==10?"100%":caseStatus<10?((caseStatus+1)*10+"%"):"") + ((chargeCase && caseStatus < 10) ? "+" : ""));
                    }else{
                        notificationBig.setViewVisibility(R.id.leftPodText, View.INVISIBLE);
                        notificationBig.setViewVisibility(R.id.rightPodText, View.INVISIBLE);
                        notificationBig.setViewVisibility(R.id.podCaseText, View.INVISIBLE);
                        notificationBig.setViewVisibility(R.id.leftPodUpdating, View.VISIBLE);
                        notificationBig.setViewVisibility(R.id.rightPodUpdating, View.VISIBLE);
                        notificationBig.setViewVisibility(R.id.podCaseUpdating, View.VISIBLE);
                        notificationSmall.setViewVisibility(R.id.leftPodText, View.INVISIBLE);
                        notificationSmall.setViewVisibility(R.id.rightPodText, View.INVISIBLE);
                        notificationSmall.setViewVisibility(R.id.podCaseText, View.INVISIBLE);
                        notificationSmall.setViewVisibility(R.id.leftPodUpdating, View.VISIBLE);
                        notificationSmall.setViewVisibility(R.id.rightPodUpdating, View.VISIBLE);
                        notificationSmall.setViewVisibility(R.id.podCaseUpdating, View.VISIBLE);
                    }
                    mNotifyManager.notify(1,mBuilder.build());
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { }
            }
        }
    }

    public PodsService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BroadcastReceiver btReceiver=null;

    /**
     * When the service is created, we register to get as many bluetooth and airpods related events as possible.
     * ACL_CONNECTED and ACL_DISCONNECTED should have been enough, but you never know with android these days.
     */
    @Override
    public void onCreate() {
        super.onCreate();
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
        try{
            unregisterReceiver(btReceiver);
        }catch (Throwable t){}
        btReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                String action = intent.getAction();
                if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                    int state= intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if(state==BluetoothAdapter.STATE_OFF||state==BluetoothAdapter.STATE_TURNING_OFF){ //bluetooth turned off, stop scanner and remove notification
                        if(ENABLE_LOGGING) Log.d(TAG,"BT OFF");
                        maybeConnected =false;
                        stopAirPodsScanner();
                        recentBeacons.clear();
                    }
                    if(state==BluetoothAdapter.STATE_ON){ //bluetooth turned on, start/restart scanner
                        if(ENABLE_LOGGING) Log.d(TAG,"BT ON");
                        startAirPodsScanner();
                    }
                }
                if (bluetoothDevice != null && action != null && !action.isEmpty()&&checkUUID(bluetoothDevice)){ //airpods filter
                    if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){ //airpods connected, show notification
                        if(ENABLE_LOGGING) Log.d(TAG,"ACL CONNECTED");
                        maybeConnected =true;
                    }
                    if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)||action.equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)){ //airpods disconnected, remove notification but leave the scanner going
                        if(ENABLE_LOGGING) Log.d(TAG,"ACL DISCONNECTED");
                        maybeConnected =false;
                        recentBeacons.clear();
                    }
                }
            }
        };
        try{
            registerReceiver(btReceiver,intentFilter);
        }catch(Throwable t){}
        //this BT Profile Proxy allows us to know if airpods are already connected when the app is started. It also fires an event when BT is turned off, in case the BroadcastReceiver doesn't do its job
        BluetoothAdapter ba=((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        ba.getProfileProxy(getApplicationContext(), new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
                if(i==BluetoothProfile.HEADSET){
                    if(ENABLE_LOGGING) Log.d(TAG,"BT PROXY SERVICE CONNECTED");
                    BluetoothHeadset h=(BluetoothHeadset)bluetoothProfile;
                    for(BluetoothDevice d:h.getConnectedDevices()){
                        if(checkUUID(d)){
                            if(ENABLE_LOGGING) Log.d(TAG,"BT PROXY: AIRPODS ALREADY CONNECTED");
                            maybeConnected=true;
                            break;
                        }
                    }
                }
            }

            @Override
            public void onServiceDisconnected(int i) {
                if(i==BluetoothProfile.HEADSET){
                    if(ENABLE_LOGGING) Log.d(TAG,"BT PROXY SERVICE DISCONNECTED ");
                    maybeConnected=false;
                }

            }
        },BluetoothProfile.HEADSET);
        if(ba.isEnabled())startAirPodsScanner(); //if BT is already on when the app is started, start the scanner without waiting for an event to happen
    }

    private boolean checkUUID(BluetoothDevice bluetoothDevice){
        ParcelUuid[] AIRPODS_UUIDS= {
                ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
                ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74")
        };
        ParcelUuid[] uuids = bluetoothDevice.getUuids();
        if(uuids==null) return false;
        for(ParcelUuid u:uuids){
            for(ParcelUuid v:AIRPODS_UUIDS){
                if(u.equals(v)) return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(btReceiver!=null) unregisterReceiver(btReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(n==null||!n.isAlive()){
            n=new NotificationThread();
            n.start();
        }
        return START_STICKY;
    }

}
