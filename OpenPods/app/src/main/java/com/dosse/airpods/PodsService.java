package com.dosse.airpods;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

public class PodsService extends Service {

    private static NotificationThread n=null;
    private static BluetoothLeScanner btScanner;
    private static int leftStatus=15, rightStatus=15, caseStatus=15;
    private static boolean chargeL=false, chargeR=false, chargeCase=false;
    private static long lastSeenConnected=0;
    private static final long TIMEOUT_CONNECTED=30000;
    private static boolean maybeConnected =true;

    private void startAirPodsScanner() {
        try {
            BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter btAdapter = btManager.getAdapter();
            btScanner = btAdapter.getBluetoothLeScanner();
            if (btAdapter == null) throw new Exception("No BT");
            if (!btAdapter.isEnabled()) throw new Exception("BT Off");
            btScanner.startScan(new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    try {
                        byte[] data = result.getScanRecord().getManufacturerSpecificData(76);
                        if (data == null) return;
                        String a = decodeHex(data);
                        if (a.length() < 16) return;
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
                        Log.d("PODS", "" + t);
                    }
                }
            });
        } catch (Throwable t) {
            Log.d("PODS", "" + t);
        }
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

    private static final String TAG="AirPods";
    private class NotificationThread extends Thread{
        public void run(){
            boolean notificationShowing=false;
            RemoteViews notificationBig=new RemoteViews(getPackageName(),R.layout.status_big);
            RemoteViews notificationSmall=new RemoteViews(getPackageName(),R.layout.status_small);
            NotificationManager mNotifyManager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder=new NotificationCompat.Builder(PodsService.this,TAG);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //on oreo and newer, create a notification channel
                NotificationChannel channel = new NotificationChannel(TAG, TAG, NotificationManager.IMPORTANCE_LOW);
                channel.enableVibration(false);
                channel.enableLights(false);
                channel.setShowBadge(true);
                mNotifyManager.createNotificationChannel(channel);
            }
            mBuilder.setCustomContentView(notificationSmall);
            mBuilder.setCustomBigContentView(notificationBig);
            mBuilder.setShowWhen(false);
            mBuilder.setOngoing(true);
            mBuilder.setSmallIcon(R.drawable.left_pod);
            for(;;){
                if(maybeConnected &&!(leftStatus==15&&rightStatus==15&&caseStatus==15)){
                    if(!notificationShowing){
                        Log.d("PODS","Creating notification");
                        notificationShowing=true;
                        mNotifyManager.notify(1,mBuilder.build());
                    }
                }else{
                    if(notificationShowing){
                        Log.d("PODS","Removing notification");
                        notificationShowing=false;
                        mNotifyManager.cancel(1);
                    }
                }
                if(notificationShowing){
                    Log.d("PODS","Left: "+leftStatus+(chargeL?"+":"")+" "+"Right: "+rightStatus+(chargeR?"+":"")+" "+"Case: "+caseStatus+(chargeCase?"+":""));
                    notificationBig.setImageViewResource(R.id.leftPodImg,leftStatus<=10?R.drawable.left_pod:R.drawable.left_pod_disconnected);
                    notificationBig.setImageViewResource(R.id.rightPodImg,rightStatus<=10?R.drawable.right_pod:R.drawable.right_pod_disconnected);
                    notificationBig.setImageViewResource(R.id.podCaseImg,caseStatus<=10?R.drawable.pod_case:R.drawable.pod_case_disconnected);
                    if(System.currentTimeMillis()-lastSeenConnected<TIMEOUT_CONNECTED) {
                        notificationBig.setTextViewText(R.id.leftPodText, (leftStatus <= 10 ? (leftStatus * 10) + "%" : "") + ((chargeL && leftStatus < 10) ? "+" : ""));
                        notificationBig.setTextViewText(R.id.rightPodText, (rightStatus <= 10 ? (rightStatus * 10) + "%" : "") + ((chargeR && rightStatus < 10) ? "+" : ""));
                        notificationBig.setTextViewText(R.id.podCaseText, (caseStatus <= 10 ? (caseStatus * 10) + "%" : "") + (chargeCase ? "+" : ""));
                        notificationSmall.setTextViewText(R.id.leftPodText, (leftStatus <= 10 ? (leftStatus * 10) + "%" : "") + (chargeL ? "+" : ""));
                        notificationSmall.setTextViewText(R.id.rightPodText, (rightStatus <= 10 ? (rightStatus * 10) + "%" : "") + (chargeR ? "+" : ""));
                        notificationSmall.setTextViewText(R.id.podCaseText, (caseStatus <= 10 ? (caseStatus * 10) + "%" : "") + (chargeCase ? "+" : ""));
                    }else{
                        //haven't received an update in a while (screen off), wait for an update before showing battery%
                        notificationBig.setTextViewText(R.id.leftPodText, "");
                        notificationBig.setTextViewText(R.id.rightPodText, "");
                        notificationBig.setTextViewText(R.id.podCaseText, "");
                        notificationSmall.setTextViewText(R.id.leftPodText, "");
                        notificationSmall.setTextViewText(R.id.rightPodText, "");
                        notificationSmall.setTextViewText(R.id.podCaseText, "");
                    }
                    mNotifyManager.notify(1,mBuilder.build());
                }
                sleepMs(1000);
            }
        }
    }

    private static void sleepMs(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) { }
    }

    public PodsService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BroadcastReceiver btReceiver=null;

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
        btReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                String action = intent.getAction();
                if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                    int state= intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if(state==BluetoothAdapter.STATE_OFF){
                        maybeConnected =false;
                    }
                    if(state==BluetoothAdapter.STATE_ON){
                        startAirPodsScanner();
                    }
                }
                if (bluetoothDevice != null && action != null && !action.isEmpty()&&checkUUID(bluetoothDevice)){
                    if(action.equals("android.bluetooth.device.action.ACL_CONNECTED")){
                        maybeConnected =true;
                        startAirPodsScanner();
                    }
                    if(action.equals("android.bluetooth.device.action.ACL_DISCONNECTED")){
                        maybeConnected =false;
                    }
                }
            }
        };
        try{
            unregisterReceiver(btReceiver);
        }catch (Throwable t){}
        try{
            registerReceiver(btReceiver,intentFilter);
        }catch(Throwable t){}
        startAirPodsScanner();
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
        startAirPodsScanner();
        return START_STICKY;
    }


}
