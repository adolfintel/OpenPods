package com.dosse.airpods;

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

import static com.dosse.airpods.AboutActivity.donateURL;
import static com.dosse.airpods.AboutActivity.fdroidURL;
import static com.dosse.airpods.AboutActivity.githubURL;
import static com.dosse.airpods.AboutActivity.websiteURL;

public class SettingsFragment extends PreferenceFragmentCompat {

    private Context context;

    @SuppressWarnings("FieldCanBeLocal")
    private Preference mAboutPreference, mHideAppPreference, mFDroidPreference, mWebsitePreference, mGithubPreference, mDonationPreference;

    @Override
    public void onCreatePreferences (Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_screen, rootKey);
        context = getContext();

        mHideAppPreference = getPreferenceManager().findPreference("hideApp");
        assert mHideAppPreference != null;
        mHideAppPreference.setOnPreferenceClickListener(preference -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.hide_dialog);
            builder.setMessage(R.string.hide_dialog_desc);
            builder.setPositiveButton(R.string.hide_dialog_button, (dialog, which) -> {
                PackageManager p = requireContext().getPackageManager();
                p.setComponentEnabledSetting(new ComponentName(context, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                Toast.makeText(context, getString(R.string.hideClicked), Toast.LENGTH_LONG).show();

                try {
                    context.openFileOutput("hidden", Context.MODE_PRIVATE).close();
                } catch (Throwable ignored) {
                }

                enableDisableOptions();
                requireActivity().finish();
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.show();
            return true;
        });

        mAboutPreference = getPreferenceManager().findPreference("about");
        assert mAboutPreference != null;
        mAboutPreference.setSummary(String.format("%s v%s", getString(R.string.app_name), BuildConfig.VERSION_NAME));
        mAboutPreference.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(context, AboutActivity.class));
            return true;
        });

        mFDroidPreference = getPreferenceManager().findPreference("fdroid");
        assert mFDroidPreference != null;
        mFDroidPreference.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fdroidURL)));
            return true;
        });

        mGithubPreference = getPreferenceManager().findPreference("github");
        assert mGithubPreference != null;
        mGithubPreference.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(githubURL)));
            return true;
        });

        mWebsitePreference = getPreferenceManager().findPreference("website");
        assert mWebsitePreference != null;
        mWebsitePreference.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(websiteURL)));
            return true;
        });

        mDonationPreference = getPreferenceManager().findPreference("donate");
        assert mDonationPreference != null;
        mDonationPreference.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(donateURL)));
            Toast.makeText(getContext(), "❤️", Toast.LENGTH_SHORT).show();
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