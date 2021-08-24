package com.dosse.airpods.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dosse.airpods.R;

public class AboutActivity extends AppCompatActivity {


    public static final String websiteURL = "https://fdossena.com/?p=openPods/index.frag";
    public static final String githubURL = "https://github.com/adolfintel/OpenPods";
    public static final String donateURL = "https://paypal.me/sineisochronic";
    public static final String fdroidURL = "https://f-droid.org/packages/com.dosse.airpods/";

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // --------------- LEGAL DANGER ZONE! --------------- //

        /*
         * The following actions are a violation of the GNU GPLv3 License:
         * - Removing, hiding or altering the license or the code that displays it
         * - Removing or changing the link to the original source code or the code that displays it
         * - Removing or changing the donation link of the original project or the code that displays it
         *
         * If you're forking the application, it MUST be made clear that your version is a fork,
         * who the original author is, and all references to the original project and license must not be removed.
         * You are free to add links to your project, donations, whatever to this activity.
         *
         * I am constantly monitoring all major Android app stores for violations. You have been warned.
         */

        ((WebView)(findViewById(R.id.license))).loadUrl("file:///android_asset/license.html");

        findViewById(R.id.website).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(websiteURL))));
        findViewById(R.id.github).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(githubURL))));
        findViewById(R.id.donate).setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(donateURL)));
            Toast.makeText(AboutActivity.this, "❤️", Toast.LENGTH_SHORT).show();
        });

        // ------------ END OF LEGAL DANGER ZONE ------------ //
    }

}
