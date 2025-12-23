package com.example.gerin.inventory;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AboutActivity extends AppCompatActivity {

    private static final String REPO_URL = "https://github.com/mukesh22584/Inventory_Price";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setupToolbar();

        TextView appVersion = findViewById(R.id.app_version);
        Button btnSourceCode = findViewById(R.id.btn_view_source);
        Button btnReleases = findViewById(R.id.btn_view_changelog);
        Button btnShare = findViewById(R.id.btn_share_app);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            appVersion.setText("Version " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            appVersion.setText("Version 1.0"); 
        }

        btnSourceCode.setOnClickListener(v -> openUrl(REPO_URL));
        
        btnReleases.setOnClickListener(v -> {
            Toast.makeText(this, "Checking for latest releases...", Toast.LENGTH_SHORT).show();
            openUrl(REPO_URL + "/releases");
        });

        btnShare.setOnClickListener(v -> shareApp());
    }

    private void shareApp() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Inventory Price App");
        intent.putExtra(Intent.EXTRA_TEXT, "Check out this Inventory price management app: " + REPO_URL);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_about);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("About");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
