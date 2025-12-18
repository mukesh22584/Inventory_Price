package com.example.gerin.inventory;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.example.gerin.inventory.Search.CustomSuggestionsAdapter;
import com.example.gerin.inventory.Search.SearchResult;
import com.example.gerin.inventory.data.ItemContract.ItemEntry;
import com.example.gerin.inventory.data.ItemCursorAdapter;
import com.example.gerin.inventory.data.ItemDbHelper;
import com.mancj.materialsearchbar.MaterialSearchBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ITEM_LOADER = 0;
    private static final int STORAGE_PERMISSION_CODE = 100;

    private ItemCursorAdapter mCursorAdapter;
    private MaterialSearchBar materialSearchBar;
    private CustomSuggestionsAdapter customSuggestionsAdapter;
    private ItemDbHelper database;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private static final String[] BACKUP_COLUMNS = {
            ItemEntry.COLUMN_ITEM_NAME, ItemEntry.COLUMN_ITEM_QUANTITY,
            ItemEntry.COLUMN_ITEM_UNIT, ItemEntry.COLUMN_ITEM_PRICE,
            ItemEntry.COLUMN_ITEM_CURRENCY, ItemEntry.COLUMN_ITEM_DESCRIPTION,
            ItemEntry.COLUMN_ITEM_TAG1, ItemEntry.COLUMN_ITEM_TAG2,
            ItemEntry.COLUMN_ITEM_TAG3, ItemEntry.COLUMN_ITEM_URI
    };

    private final ActivityResultLauncher<Intent> restoreDataLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    processRestoreUri(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        setupToolbar();
        setupDatabaseAndSearch();
        setupListView();

        findViewById(R.id.catalog_fab).setOnClickListener(v -> 
                startActivity(new Intent(this, EditorActivity.class)));

        LoaderManager.getInstance(this).initLoader(ITEM_LOADER, null, this);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    private void setupDatabaseAndSearch() {
        database = new ItemDbHelper(this);
        materialSearchBar = findViewById(R.id.search_bar1);
        if (materialSearchBar != null) {
            customSuggestionsAdapter = new CustomSuggestionsAdapter(LayoutInflater.from(this), database);
            materialSearchBar.setCustomSuggestionAdapter(customSuggestionsAdapter);

            materialSearchBar.addTextChangeListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    handler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> {
                        if (customSuggestionsAdapter != null) {
                            customSuggestionsAdapter.getFilter().filter(s.toString());
                        }
                    };
                    handler.postDelayed(searchRunnable, 300);
                }
            });

            materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
                @Override
                public void onSearchStateChanged(boolean enabled) {
                    if (enabled && customSuggestionsAdapter != null) 
                        customSuggestionsAdapter.getFilter().filter("");
                }

                @Override
                public void onSearchConfirmed(CharSequence text) {
                    List<SearchResult> results = database.getNewResult(text.toString());
                    if (!results.isEmpty()) launchItemActivity(results.get(0).getId());
                }

                @Override public void onButtonClicked(int buttonCode) {}
            });
        }
    }

    private void setupListView() {
        ListView itemListView = findViewById(R.id.catalog_list);
        View emptyView = findViewById(R.id.empty_view);
        if (itemListView != null) {
            itemListView.setEmptyView(emptyView);
            mCursorAdapter = new ItemCursorAdapter(this, null);
            itemListView.setAdapter(mCursorAdapter);
            itemListView.setOnItemClickListener((parent, view, position, id) -> launchItemActivity(id));
        }
    }

    private void launchItemActivity(long id) {
        Intent intent = new Intent(this, ItemActivity.class);
        intent.setData(ContentUris.withAppendedId(ItemEntry.CONTENT_URI, id));
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete_all_entries) {
            showDeleteAllConfirmationDialog();
            return true;
        } else if (id == R.id.action_sort_by) {
            showSortByDialog();
            return true;
        } else if (id == R.id.action_theme_switcher) {
            toggleTheme();
            return true;
        } else if (id == R.id.action_backup) {
            checkPermissionAndRun(this::backupData);
            return true;
        } else if (id == R.id.action_restore) {
            checkPermissionAndRun(this::restoreData);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleTheme() {
        int nightMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) 
                ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        String[] projection = {
                ItemEntry._ID, ItemEntry.COLUMN_ITEM_NAME, ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_PRICE, ItemEntry.COLUMN_ITEM_UNIT,
                ItemEntry.COLUMN_ITEM_CURRENCY, ItemEntry.COLUMN_ITEM_IMAGE
        };
        String sortOrder = (bundle != null) ? bundle.getString("sortOrder") : null;
        return new CursorLoader(this, ItemEntry.CONTENT_URI, projection, null, null, sortOrder);
    }

    @Override public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) { 
        if (mCursorAdapter != null) mCursorAdapter.swapCursor(data); 
    }
    
    @Override public void onLoaderReset(@NonNull Loader<Cursor> loader) { 
        if (mCursorAdapter != null) mCursorAdapter.swapCursor(null); 
    }

    private void backupData() {
        try (Cursor cursor = getContentResolver().query(ItemEntry.CONTENT_URI, null, null, null, null)) {
            if (cursor == null) return;
            JSONArray jsonArray = new JSONArray();
            while (cursor.moveToNext()) {
                JSONObject json = new JSONObject();
                for (String col : BACKUP_COLUMNS) {
                    int idx = cursor.getColumnIndex(col);
                    if (idx != -1) json.put(col, cursor.getString(idx));
                }
                int imgIdx = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE);
                if (imgIdx != -1 && !cursor.isNull(imgIdx)) {
                    json.put(ItemEntry.COLUMN_ITEM_IMAGE, Base64.encodeToString(cursor.getBlob(imgIdx), Base64.DEFAULT));
                }
                jsonArray.put(json);
            }
            saveBackupFile(jsonArray.toString());
        } catch (Exception e) {
            showToast("Backup failed: " + e.getMessage());
        }
    }

    private void saveBackupFile(String data) throws IOException {
        String timeStr = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String fileName = "inventory_" + timeStr + ".json";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out != null) {
                        out.write(data.getBytes());
                        showToast("Saved to Downloads");
                    }
                }
            }
        } else {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(data);
                showToast("Backup successful");
            }
        }
    }

    private void restoreData() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        restoreDataLauncher.launch(intent);
    }

    private void processRestoreUri(Uri uri) {
        try {
            StringBuilder sb = new StringBuilder();
            try (InputStream in = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            restoreFromJson(sb.toString());
        } catch (Exception e) {
            showToast("Restore failed");
        }
    }

    private void restoreFromJson(String json) throws JSONException {
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            ContentValues values = new ContentValues();
            String name = obj.getString(ItemEntry.COLUMN_ITEM_NAME);
            
            values.put(ItemEntry.COLUMN_ITEM_NAME, name);
            values.put(ItemEntry.COLUMN_ITEM_QUANTITY, obj.optInt(ItemEntry.COLUMN_ITEM_QUANTITY));
            values.put(ItemEntry.COLUMN_ITEM_PRICE, obj.optDouble(ItemEntry.COLUMN_ITEM_PRICE));
            if (obj.has(ItemEntry.COLUMN_ITEM_IMAGE)) {
                values.put(ItemEntry.COLUMN_ITEM_IMAGE, Base64.decode(obj.getString(ItemEntry.COLUMN_ITEM_IMAGE), Base64.DEFAULT));
            }

            try (Cursor c = getContentResolver().query(ItemEntry.CONTENT_URI, new String[]{ItemEntry._ID}, 
                    ItemEntry.COLUMN_ITEM_NAME + "=?", new String[]{name}, null)) {
                if (c != null && c.moveToFirst()) {
                    Uri itemUri = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, c.getLong(0));
                    getContentResolver().update(itemUri, values, null, null);
                } else {
                    getContentResolver().insert(ItemEntry.CONTENT_URI, values);
                }
            }
        }
        LoaderManager.getInstance(this).restartLoader(ITEM_LOADER, null, this);
        showToast("Restore Complete");
    }

    private void showSortByDialog() {
        String[] options = {"Default", "Name (A-Z)", "Price (Low-High)"};
        String[] orders = {null, ItemEntry.COLUMN_ITEM_NAME + " ASC", ItemEntry.COLUMN_ITEM_PRICE + " ASC"};
        
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle(R.string.sort_dialog_msg)
                .setSingleChoiceItems(options, 0, (d, which) -> {
                    Bundle b = new Bundle();
                    b.putString("sortOrder", orders[which]);
                    LoaderManager.getInstance(this).restartLoader(ITEM_LOADER, b, this);
                    d.dismiss();
                }).show();
    }

    private void showDeleteAllConfirmationDialog() {
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setMessage(R.string.delete_all_dialog_msg)
                .setPositiveButton(R.string.delete, (d, id) -> {
                    getContentResolver().delete(ItemEntry.CONTENT_URI, null, null);
                    showToast(getString(R.string.editor_delete_all_items_successful));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void checkPermissionAndRun(Runnable action) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && 
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else {
            action.run();
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (materialSearchBar != null && materialSearchBar.isSuggestionsVisible()) {
            materialSearchBar.closeSearch();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null) database.close();
    }

    private abstract static class SimpleTextWatcher implements android.text.TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(android.text.Editable s) {}
    }
}
