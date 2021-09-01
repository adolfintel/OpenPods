package com.dosse.airpods.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.dosse.airpods.BuildConfig;
import com.dosse.airpods.R;
import com.dosse.airpods.receivers.StartupReceiver;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences (Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preference_screen, rootKey);

            getPreference("batterySaver").setOnPreferenceClickListener(preference -> {
                StartupReceiver.restartPodsService(requireContext());
                return true;
            });

            getPreference("restartService").setOnPreferenceClickListener(preference -> {
                StartupReceiver.restartPodsService(requireContext());
                return true;
            });

            getPreference("about").setSummary(String.format("%s v%s", getString(R.string.app_name), BuildConfig.VERSION_NAME));

            getPreference("donate").setOnPreferenceClickListener(preference -> {
                Toast.makeText(requireContext(), "❤️", Toast.LENGTH_SHORT).show();
                return false;
            });
        }

        private Preference getPreference (String key) {
            return getPreferenceManager().findPreference(key);
        }

    }

}
