package com.dosse.airpods;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Timer;
import java.util.TimerTask;

public class API30Activity extends AppCompatActivity {

    private Timer timer;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api30);

        // Allow button clicked, ask for permissions
        findViewById(R.id.allowBtn).setOnClickListener(view -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });


        // Wait for permissions to be granted.
        // When they are granted, go to MainActivity.
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run () {
                if (ContextCompat.checkSelfPermission(API30Activity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    timer.cancel();
                    startActivity(new Intent(API30Activity.this, MainActivity.class));
                    finish();
                }
            }
        }, 0, 100);

    }

    // Activity destroyed (or screen rotated). destroy the timer too
    @Override
    protected void onDestroy () {
        super.onDestroy();
        if (timer != null)
            timer.cancel();
    }

}
