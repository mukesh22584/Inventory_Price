package com.example.gerin.inventory;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 100;
    private AlertDialog progressDialog;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> restoreDataLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    processRestoreUri(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView appInfoText = findViewById(R.id.settings_app_info);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            appInfoText.setText(getString(R.string.app_name) + " - " + version);
        } catch (PackageManager.NameNotFoundException e) {
            appInfoText.setText(getString(R.string.app_name));
        }

        findViewById(R.id.btn_theme_settings).setOnClickListener(v -> showThemeDialog());
        findViewById(R.id.btn_backup_settings).setOnClickListener(v -> checkPermissionAndRun(this::backupData));
        findViewById(R.id.btn_restore_settings).setOnClickListener(v -> checkPermissionAndRun(this::restoreData));
        findViewById(R.id.btn_about_settings).setOnClickListener(v -> showAboutDialog());

	}

    private void showThemeDialog() {
        String[] themeOptions = {"Light", "Dark", "System Default"};
        int[] modeValues = {
                AppCompatDelegate.MODE_NIGHT_NO,
                AppCompatDelegate.MODE_NIGHT_YES,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        };

        SharedPreferences prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        int currentSavedMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        int checkedItem = 2;
        for (int i = 0; i < modeValues.length; i++) {
            if (modeValues[i] == currentSavedMode) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themeOptions, 2, (dialog, which) -> {
                    prefs.edit().putInt("theme_mode", modeValues[which]).apply();
                    AppCompatDelegate.setDefaultNightMode(modeValues[which]);
                    dialog.dismiss();
                    recreate();
                }).show();
    }

    private void backupData() {
        showLoading("Please wait....");
        handler.postDelayed(() -> {
            File dbFile = getDatabasePath("Inventory.db");
            String fileName = "inventory_backup_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date()) + ".db";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/InventoryBackups");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                try (InputStream in = new FileInputStream(dbFile); OutputStream out = getContentResolver().openOutputStream(uri)) {
                    byte[] buf = new byte[1024]; int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                    hideLoading();
                    Toast.makeText(this, "Backup saved to Downloads/InventoryBackups", Toast.LENGTH_SHORT).show();
                } catch (IOException e) { hideLoading(); }
            }
        }, 500);
    }

    private void restoreData() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/document/primary:Download%2FInventoryBackups"));
        }
        restoreDataLauncher.launch(intent);
    }

    private void processRestoreUri(Uri uri) {
        showLoading("Please wait....");
        handler.postDelayed(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(getDatabasePath("Inventory.db"))) {
                byte[] buf = new byte[1024]; int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                hideLoading();
                Toast.makeText(this, "Restore complete", Toast.LENGTH_SHORT).show();
            } catch (IOException e) { hideLoading(); }
        }, 500);
    }

    private void showAboutDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.about_content, null);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) dialogView.findViewById(R.id.app_version)).setText("Version " + pInfo.versionName);
        } catch (Exception e) { }
        
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showLoading(String message) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        ((TextView) dialogView.findViewById(R.id.progress_message)).setText(message);
        progressDialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme).setView(dialogView).setCancelable(false).create();
        if (progressDialog.getWindow() != null) progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        progressDialog.show();
    }

    private void hideLoading() { if (progressDialog != null) progressDialog.dismiss(); }

    private void checkPermissionAndRun(Runnable action) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else action.run();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
