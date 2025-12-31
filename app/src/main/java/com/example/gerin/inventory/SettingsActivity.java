package com.example.gerin.inventory;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gerin.inventory.data.ItemContract.ItemEntry;
import com.example.gerin.inventory.data.ItemDbHelper;

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

        updateItemCount();

        findViewById(R.id.btn_reset_data).setOnClickListener(v -> confirmReset());

        SharedPreferences prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);

        LinearLayout themeContainer = findViewById(R.id.theme_options_container);
        findViewById(R.id.btn_theme_header).setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition((ViewGroup) themeContainer.getParent(), new AutoTransition());
            if (themeContainer.getVisibility() == View.VISIBLE) {
                themeContainer.setVisibility(View.GONE);
            } else {
                themeContainer.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.btn_theme_light).setOnClickListener(v -> 
                updateTheme(prefs, AppCompatDelegate.MODE_NIGHT_NO));

        findViewById(R.id.btn_theme_dark).setOnClickListener(v -> 
                updateTheme(prefs, AppCompatDelegate.MODE_NIGHT_YES));

        findViewById(R.id.btn_theme_system).setOnClickListener(v -> 
                updateTheme(prefs, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));

        findViewById(R.id.btn_backup_settings).setOnClickListener(v -> checkPermissionAndRun(this::backupData));
        findViewById(R.id.btn_restore_settings).setOnClickListener(v -> checkPermissionAndRun(this::restoreData));

        LinearLayout aboutContainer = findViewById(R.id.about_options_container);
        findViewById(R.id.btn_about_header).setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition((ViewGroup) aboutContainer.getParent(), new AutoTransition());
            if (aboutContainer.getVisibility() == View.VISIBLE) {
                aboutContainer.setVisibility(View.GONE);
            } else {
                aboutContainer.setVisibility(View.VISIBLE);
            }
        });

        final String repoUrl = "https://github.com/mukesh22584/Inventory_Price";

        findViewById(R.id.btn_changelog).setOnClickListener(v -> 
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl + "/releases"))));

        findViewById(R.id.btn_view_source).setOnClickListener(v -> 
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl))));

        findViewById(R.id.btn_share_app).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this app: " + repoUrl);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });
    }

    private void updateItemCount() {
        TextView countText = findViewById(R.id.app_total_items_count);
        TextView sizeText = findViewById(R.id.app_database_size);
        TextView backupText = findViewById(R.id.app_backup_status);

        ItemDbHelper dbHelper = new ItemDbHelper(this);
        File dbFile = getDatabasePath("Inventory.db");

        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            long count = DatabaseUtils.queryNumEntries(db, ItemEntry.TABLE_NAME);
            countText.setText("Total Products: " + count);
        } catch (Exception e) { countText.setText("Total Products: 0"); }

        if (dbFile.exists()) {
            double sizeInKB = dbFile.length() / 1024.0;
            sizeText.setText(String.format(Locale.getDefault(), "Database Size: %.2f KB", sizeInKB));
        }

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String lastBackup = prefs.getString("last_backup", "Never");
        backupText.setText("Last Backup: " + lastBackup);
    }

    private void confirmReset() {
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setTitle("Delete All Items?")
            .setMessage("This will permanently delete all your products and details. This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, id) -> {
                int rowsDeleted = getContentResolver().delete(ItemEntry.CONTENT_URI, null, null);
                if (rowsDeleted >= 0) {
                    updateItemCount();
                    Toast.makeText(this, "All data deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error deleting data", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateTheme(SharedPreferences prefs, int mode) {
        prefs.edit().putInt("theme_mode", mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
        recreate();
    }

    private void backupData() {
        showLoading("Please wait....");
        handler.postDelayed(() -> {
            File dbFile = getDatabasePath("Inventory.db");
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
            String fileName = "inventory_backup_" + timeStamp + ".db";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/InventoryBackups");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                try (InputStream in = new FileInputStream(dbFile); OutputStream out = getContentResolver().openOutputStream(uri)) {
                    byte[] buf = new byte[1024]; int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);

                    String displayTime = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("last_backup", displayTime).apply();

                    hideLoading();
                    updateItemCount();
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
        showLoading("Merging backup data...");
        handler.postDelayed(() -> {
            File tempDbFile = new File(getCacheDir(), "temp_backup.db");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(tempDbFile)) {

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);

                SQLiteDatabase backupDb = SQLiteDatabase.openDatabase(tempDbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = backupDb.query(ItemEntry.TABLE_NAME, null, null, null, null, null, null);

                int mergedCount = 0;
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        ContentValues values = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(cursor, values);
                        values.remove(ItemEntry._ID);

                        String name = values.getAsString(ItemEntry.COLUMN_ITEM_NAME);

                        Cursor existing = getContentResolver().query(ItemEntry.CONTENT_URI, null,
                                ItemEntry.COLUMN_ITEM_NAME + "=?", new String[]{name}, null);

                        if (existing != null && existing.moveToFirst()) {
                            if (isDataIdentical(existing, values)) {
                                getContentResolver().update(ItemEntry.CONTENT_URI, values,
                                        ItemEntry.COLUMN_ITEM_NAME + "=?", new String[]{name});
                            } else {
                                values.put(ItemEntry.COLUMN_ITEM_NAME, name + " (Backup)");
                                getContentResolver().insert(ItemEntry.CONTENT_URI, values);
                            }
                            existing.close();
                        } else {
                            getContentResolver().insert(ItemEntry.CONTENT_URI, values);
                        }
                        mergedCount++;
                    }
                    cursor.close();
                }
                backupDb.close();
                tempDbFile.delete();

                getContentResolver().notifyChange(ItemEntry.CONTENT_URI, null);
                hideLoading();
                updateItemCount();
                Toast.makeText(this, "Merged " + mergedCount + " items successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                hideLoading();
                Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, 500);
    }

    private boolean isDataIdentical(Cursor existing, ContentValues backup) {
        String[] columns = {
                ItemEntry.COLUMN_ITEM_QUANTITY, ItemEntry.COLUMN_ITEM_UNIT,
                ItemEntry.COLUMN_ITEM_PRICE, ItemEntry.COLUMN_ITEM_CURRENCY,
                ItemEntry.COLUMN_ITEM_DESCRIPTION, ItemEntry.COLUMN_ITEM_TAG1,
                ItemEntry.COLUMN_ITEM_TAG2, ItemEntry.COLUMN_ITEM_TAG3,
                ItemEntry.COLUMN_ITEM_URI
        };

        for (String col : columns) {
            int idx = existing.getColumnIndex(col);
            if (idx != -1) {
                String existingVal = existing.getString(idx);
                String backupVal = backup.getAsString(col);

                existingVal = (existingVal == null) ? "" : existingVal;
                backupVal = (backupVal == null) ? "" : backupVal;

                if (!existingVal.equals(backupVal)) return false;
            }
        }
        return true;
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
