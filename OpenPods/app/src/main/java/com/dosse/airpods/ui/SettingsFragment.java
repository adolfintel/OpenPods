package com.dosse.airpods.ui;

import static com.dosse.airpods.ui.AboutActivity.donateURL;
import static com.dosse.airpods.ui.AboutActivity.fdroidURL;
import static com.dosse.airpods.ui.AboutActivity.githubURL;
import static com.dosse.airpods.ui.AboutActivity.websiteURL;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.dosse.airpods.BuildConfig;
import com.dosse.airpods.R;
import com.dosse.airpods.receivers.StartupReceiver;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences (Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_screen, rootKey);

        Preference mRestartPreference = getPreferenceManager().findPreference("restartService");
        Objects.requireNonNull(mRestartPreference).setOnPreferenceClickListener(preference ->  {
            StartupReceiver.restartPodsService(requireContext());
            return true;
        });

        Preference mAboutPreference = getPreferenceManager().findPreference("about");
        Objects.requireNonNull(mAboutPreference).setSummary(String.format("%s v%s", getString(R.string.app_name), BuildConfig.VERSION_NAME));
        mAboutPreference.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireContext(), AboutActivity.class));
            return true;
        });

        Preference mFDroidPreference = getPreferenceManager().findPreference("fdroid");
        Objects.requireNonNull(mFDroidPreference).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fdroidURL)));
            return true;
        });

        Preference mGithubPreference = getPreferenceManager().findPreference("github");
        Objects.requireNonNull(mGithubPreference).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(githubURL)));
            return true;
        });

        Preference mWebsitePreference = getPreferenceManager().findPreference("website");
        Objects.requireNonNull(mWebsitePreference).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(websiteURL)));
            return true;
        });

        Preference mDonationPreference = getPreferenceManager().findPreference("donate");
        Objects.requireNonNull(mDonationPreference).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(donateURL)));
            Toast.makeText(requireContext(), "❤️", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

}