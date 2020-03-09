package com.dosse.airpods;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //check if Bluetooth LE is available on this device. If not, show an error
        BluetoothAdapter btAdapter=((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if(btAdapter==null||(btAdapter.isEnabled()&&btAdapter.getBluetoothLeScanner()==null)||(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))){
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
            Starter.startPodsService(getApplicationContext());
        }else{
            Intent i=new Intent(this,IntroActivity.class);
            startActivity(i);
            finish();
        }
        ((Button)(findViewById(R.id.settings))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //settings clicked
                Intent i=new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        try{
            getApplicationContext().openFileInput("hidden").close();
            finish();
        }catch (Throwable t){}
    }
}
