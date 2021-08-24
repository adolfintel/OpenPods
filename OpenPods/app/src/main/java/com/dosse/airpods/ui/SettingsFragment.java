package com.dosse.airpods.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import static com.dosse.airpods.ui.AboutActivity.donateURL;
import static com.dosse.airpods.ui.AboutActivity.fdroidURL;
import static com.dosse.airpods.ui.AboutActivity.githubURL;
import static com.dosse.airpods.ui.AboutActivity.websiteURL;

import com.dosse.airpods.BuildConfig;
import com.dosse.airpods.R;
import com.dosse.airpods.receivers.StartupReceiver;
import com.dosse.airpods.utils.Logger;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {

    private Preference mHideAppPreference;

    @Override
    public void onCreatePreferences (Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_screen, rootKey);

        Preference mRestartPreference = getPreferenceManager().findPreference("restartService");
        Objects.requireNonNull(mRestartPreference).setOnPreferenceClickListener(preference ->  {
            StartupReceiver.restartPodsService(requireContext());
            return true;
        });

        mHideAppPreference = getPreferenceManager().findPreference("hideApp");
        Objects.requireNonNull(mHideAppPreference).setOnPreferenceClickListener(preference -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.hide_dialog);
            builder.setMessage(R.string.hide_dialog_desc);
            builder.setPositiveButton(R.string.hide_dialog_button, (dialog, which) -> {
                PackageManager p = requireContext().getPackageManager();
                p.setComponentEnabledSetting(new ComponentName(requireContext(), MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                Toast.makeText(requireContext(), getString(R.string.hideClicked), Toast.LENGTH_LONG).show();

                try {
                    requireContext().openFileOutput("hidden", Context.MODE_PRIVATE).close();
                } catch (Throwable t) {
                    Logger.error(t);
                }

                enableDisableOptions();
                requireActivity().finish();
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.show();
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

        enableDisableOptions();
    }

    private void enableDisableOptions () {
        try {
            requireContext().openFileInput("hidden").close();
            mHideAppPreference.setEnabled(false);
        } catch (Throwable t) {
            Logger.error(t);
        }
    }

}