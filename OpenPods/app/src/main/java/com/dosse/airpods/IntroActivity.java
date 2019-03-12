package com.dosse.airpods;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.util.Timer;
import java.util.TimerTask;

public class IntroActivity extends AppCompatActivity {

    private Timer t;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        ((Button)findViewById(R.id.allowBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                try {
                    if(!getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName())) {
                        Intent intent = new Intent();
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                } catch (Throwable t) {
                }
            }
        });
        t= new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                boolean ok=true;
                if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O) {
                    if(!getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName())) ok=false;
                }
                if (ContextCompat.checkSelfPermission(IntroActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ok=false;
                if(ok){
                    t.cancel();
                    Intent i=new Intent(IntroActivity.this,MainActivity.class);
                    startActivity(i);
                    finish();
                }
            }
        },0,100);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(t!=null) t.cancel();
    }
}
