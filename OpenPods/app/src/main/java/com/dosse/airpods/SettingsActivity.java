package com.dosse.airpods;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferenceManager().setSharedPreferencesName("openpods");
        addPreferencesFromResource(R.xml.pref_general);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        //hide app listener
        ((Preference)findPreference("hideApp")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                PackageManager p = getPackageManager();
                p.setComponentEnabledSetting(new ComponentName(SettingsActivity.this,MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                Toast.makeText(getApplicationContext(),getString(R.string.hideClicked), Toast.LENGTH_LONG).show();
                try{getApplicationContext().openFileOutput("hidden",MODE_PRIVATE).close();}catch(Throwable t){}
                enableDisableOptions();
                finish();
                return true;
            }
        });

        //about listener. Removing or hiding this is a violation of the GPL license
        ((Preference)findPreference("about")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i=new Intent(SettingsActivity.this,AboutActivity.class);
                startActivity(i);
                return true;
            }
        });

        enableDisableOptions();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equalsIgnoreCase("batterySaver")){
            Starter.restartPodsService(getApplicationContext());
        }
    }

    private void enableDisableOptions(){
        try{
            getApplicationContext().openFileInput("hidden").close();
            ((Preference)findPreference("hideApp")).setEnabled(false);
        }catch (Throwable t){}
    }
}
