package com.dosse.airpods;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {

    private Context context;

    @SuppressWarnings("FieldCanBeLocal")
    private Preference mAboutPreference, mHideAppPreference;

    @Override
    public void onCreatePreferences (Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_screen, rootKey);
        context = getContext();

        mAboutPreference = getPreferenceManager().findPreference("about");
        Objects.requireNonNull(mAboutPreference).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(context, AboutActivity.class));
            return true;
        });

        mHideAppPreference = getPreferenceManager().findPreference("hideApp");
        Objects.requireNonNull(mHideAppPreference).setOnPreferenceClickListener(preference -> {
            PackageManager p = requireContext().getPackageManager();
            p.setComponentEnabledSetting(new ComponentName(context, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            Toast.makeText(context, getString(R.string.hideClicked), Toast.LENGTH_LONG).show();

            try {
                context.openFileOutput("hidden", Context.MODE_PRIVATE).close();
            } catch (Throwable ignored) {
            }

            enableDisableOptions();
            requireActivity().finish();
            return true;
        });

        enableDisableOptions();
    }

    private void enableDisableOptions () {
        try {
            context.openFileInput("hidden").close();
            mHideAppPreference.setEnabled(false);
        } catch (Throwable ignored) {
        }
    }

}