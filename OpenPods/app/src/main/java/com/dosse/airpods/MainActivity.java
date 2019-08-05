package com.dosse.airpods;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //check if Bluetooth LE is available on this device. If not, show an error
        BluetoothAdapter btAdapter=((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if(btAdapter==null||(btAdapter.isEnabled()&&btAdapter.getBluetoothLeScanner()==null)){
            Intent i=new Intent(this,NoBTActivity.class);
            startActivity(i);
            finish();
            return;
        }
        //check if all permissions have been granted
        boolean ok=true;
        try {
            if (!getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName())) ok = false;
        }catch(Throwable t){}
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ok=false;
        if(ok){
            startService(new Intent(getApplicationContext(),PodsService.class));
        }else{
            Intent i=new Intent(this,IntroActivity.class);
            startActivity(i);
            finish();
        }
        ((Button)(findViewById(R.id.mainHide))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //hide app clicked
                PackageManager p = getPackageManager();
                p.setComponentEnabledSetting(new ComponentName(MainActivity.this,MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                Toast.makeText(getApplicationContext(),getString(R.string.hideClicked), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }



}
