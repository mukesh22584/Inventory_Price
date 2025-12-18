package com.example.gerin.inventory;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.ContentUris;
import androidx.loader.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.loader.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.example.gerin.inventory.Search.CustomSuggestionsAdapter;
import com.example.gerin.inventory.Search.SearchResult;
import com.example.gerin.inventory.data.ItemContract;
import com.example.gerin.inventory.data.ItemCursorAdapter;
import com.example.gerin.inventory.data.ItemDbHelper;
import com.mancj.materialsearchbar.MaterialSearchBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private ItemCursorAdapter mCursorAdapter;

    /**
     * Identifier for the item data loader
     */
    private static final int ITEM_LOADER = 0;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST = 1;
    private static final int READ_EXTERNAL_STORAGE_REQUEST = 2;

    /**
     * Variables for the search bar
     */
    MaterialSearchBar materialSearchBar;
    CustomSuggestionsAdapter customSuggestionsAdapter;
    ItemDbHelper database;
    List<SearchResult> searchResultList = new ArrayList<>();

    private final ActivityResultLauncher<Intent> restoreDataLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri uri = data.getData();
                        try {
                            String json = readFileContent(uri);
                            restoreDataFromJson(json);
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to restore data", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onStart() {
        super.onStart();
        if (materialSearchBar.isSuggestionsVisible()) {
            loadSearchResultList();
            customSuggestionsAdapter.setSuggestions(searchResultList);
            customSuggestionsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (materialSearchBar.isSuggestionsVisible()) {
            materialSearchBar.closeSearch();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create a new instance of the database for access to the searchbar
        database = new ItemDbHelper(this);

        // Create the search bar
        materialSearchBar = findViewById(R.id.search_bar1);
        loadSearchResultList();

        customSuggestionsAdapter = new CustomSuggestionsAdapter(LayoutInflater.from(this));
        customSuggestionsAdapter.setSuggestions(searchResultList);
        materialSearchBar.setCustomSuggestionAdapter(customSuggestionsAdapter);

        // Add flags to determine when to stop loading search results
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                materialSearchBar.disableSearch();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<SearchResult> newSuggestions = loadNewSearchResultList();
                customSuggestionsAdapter.setSuggestions(newSuggestions);
                customSuggestionsAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                List<SearchResult> newSuggestions = loadNewSearchResultList();
                if (!newSuggestions.isEmpty()) {
                    SearchResult result = newSuggestions.get(0);
                    int testResult3 = result.getId();

                    Intent intent = new Intent(CatalogActivity.this, ItemActivity.class);
                    Uri currentPetUri = ContentUris.withAppendedId(ItemContract.ItemEntry.CONTENT_URI, testResult3);
                    intent.setData(currentPetUri);

                    startActivity(intent);
                }
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

        // Find the ListView which will be populated with the pet data
        ListView itemListView = findViewById(R.id.catalog_list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        itemListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of pet data in the Cursor.
        // There is no pet data yet (until the loader finishes) so pass in null for the Cursor.
        mCursorAdapter = new ItemCursorAdapter(this, null);
        itemListView.setAdapter(mCursorAdapter);


        // Setup FAB to open EditorActivity
        FloatingActionButton fab = findViewById(R.id.catalog_fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
            startActivity(intent);
        });

        // Setup the item click listener
        itemListView.setOnItemClickListener((adapterView, view, position, id) -> {
                // Create new intent to go to {@link EditorActivity}
                Intent intent = new Intent(CatalogActivity.this, ItemActivity.class);

                Uri currentPetUri = ContentUris.withAppendedId(ItemContract.ItemEntry.CONTENT_URI, id);
                intent.setData(currentPetUri);

                startActivity(intent);
        });

        // Kick off the loader
        getSupportLoaderManager().initLoader(ITEM_LOADER, null, this);

    }

    private void loadSearchResultList() {
        searchResultList = database.getResults();
    }

    private List<SearchResult> loadNewSearchResultList() {
        List<SearchResult> newSuggestions = new ArrayList<>();
        loadSearchResultList();
        for (SearchResult searchResult : searchResultList) {
            if (searchResult.getName().toLowerCase().contains(materialSearchBar.getText().toLowerCase())) {
                newSuggestions.add(searchResult);
            }
        }

        return newSuggestions;

    }

    /* Methods to create menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        int itemId = item.getItemId();
        if (itemId == R.id.action_delete_all_entries) {
            showDeleteAllConfirmationDialog();
            return true;
        } else if (itemId == R.id.action_sort_by) {
            showSortByDialog();
        } else if (itemId == R.id.action_theme_switcher) {
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            recreate();
            return true;
        } else if (itemId == R.id.action_backup) {
            backupData();
            return true;
        } else if (itemId == R.id.action_restore) {
            restoreData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                ItemContract.ItemEntry._ID,
                ItemContract.ItemEntry.COLUMN_ITEM_NAME,
                ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemContract.ItemEntry.COLUMN_ITEM_PRICE,
                ItemContract.ItemEntry.COLUMN_ITEM_UNIT,
                ItemContract.ItemEntry.COLUMN_ITEM_CURRENCY,
                ItemContract.ItemEntry.COLUMN_ITEM_IMAGE
        };

        String sortOrder = null;
        if (bundle != null) {
            sortOrder = bundle.getString("sortOrder");
        }

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                ItemContract.ItemEntry.CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                sortOrder);             // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private void showSortByDialog() {
        final String[] sortOptions = {"Default", "Name (A-Z)", "Price (Low-High)"};
        final String[] sortOrderOptions = {null, ItemContract.ItemEntry.COLUMN_ITEM_NAME + " ASC", ItemContract.ItemEntry.COLUMN_ITEM_PRICE + " ASC"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RadioDialogTheme);
        builder.setTitle(R.string.sort_dialog_msg);

        int checkedItem = 0;
        builder.setSingleChoiceItems(sortOptions, checkedItem, (dialog, which) -> {
            Bundle bundle = new Bundle();
            bundle.putString("sortOrder", sortOrderOptions[which]);
            getSupportLoaderManager().restartLoader(ITEM_LOADER, bundle, CatalogActivity.this);
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteAllConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.delete, (dialog, id) -> deleteAllItems());
        builder.setNegativeButton(R.string.cancel, (dialog, id) -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteAllItems() {
        int rowsDeleted = getContentResolver().delete(ItemContract.ItemEntry.CONTENT_URI, null, null);
        if (rowsDeleted > 0) {
            Toast.makeText(this, R.string.editor_delete_all_items_successful, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.editor_delete_all_items_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
    }

    private void backupData() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST);
            return;
        }

        Cursor cursor = getContentResolver().query(ItemContract.ItemEntry.CONTENT_URI, null, null, null, null);
        if (cursor == null) {
            Toast.makeText(this, "No data to backup", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONArray jsonArray = new JSONArray();
        while (cursor.moveToNext()) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_NAME, cursor.getString(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_NAME)));
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY, cursor.getInt(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY)));
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_UNIT, cursor.getString(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_UNIT)));
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_PRICE, cursor.getDouble(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_PRICE)));
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_CURRENCY, cursor.getString(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_CURRENCY)));
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_DESCRIPTION, cursor.getString(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_DESCRIPTION)));
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG1, cursor.getString(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_TAG1)));
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG2, cursor.getString(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_TAG2)));
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG3, cursor.getString(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_TAG3)));
                byte[] imageBytes = cursor.getBlob(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_IMAGE));
                String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_IMAGE, imageString);
                jsonObject.put(ItemContract.ItemEntry.COLUMN_ITEM_URI, cursor.getString(cursor.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_URI)));
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        cursor.close();

        try {
            @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "inventory_" + timeStamp + ".json";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                        outputStream.write(jsonArray.toString().getBytes());
                        Toast.makeText(this, "Backup successful", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to create backup", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Failed to create backup", Toast.LENGTH_SHORT).show();
                }
            } else {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(jsonArray.toString());
                fileWriter.flush();
                fileWriter.close();
                Toast.makeText(this, "Backup successful", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to create backup", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreData() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        restoreDataLauncher.launch(intent);
    }

    private String readFileContent(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
             java.io.BufferedReader reader = new java.io.BufferedReader(
                     new java.io.InputStreamReader(java.util.Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    private void restoreDataFromJson(String json) throws JSONException {
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            ContentValues values = new ContentValues();
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_NAME, jsonObject.getString(ItemContract.ItemEntry.COLUMN_ITEM_NAME));
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY, jsonObject.getInt(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY));
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_UNIT, jsonObject.getString(ItemContract.ItemEntry.COLUMN_ITEM_UNIT));
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_PRICE, jsonObject.getDouble(ItemContract.ItemEntry.COLUMN_ITEM_PRICE));
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_CURRENCY, jsonObject.getString(ItemContract.ItemEntry.COLUMN_ITEM_CURRENCY));
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_DESCRIPTION, jsonObject.getString(ItemContract.ItemEntry.COLUMN_ITEM_DESCRIPTION));
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG1, jsonObject.getString(ItemContract.ItemEntry.COLUMN_ITEM_TAG1));
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG2, jsonObject.getString(ItemContract.ItemEntry.COLUMN_ITEM_TAG2));
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG3, jsonObject.getString(ItemContract.ItemEntry.COLUMN_ITEM_TAG3));
            String imageString = jsonObject.getString(ItemContract.ItemEntry.COLUMN_ITEM_IMAGE);
            byte[] imageBytes = Base64.decode(imageString, Base64.DEFAULT);
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_IMAGE, imageBytes);
            values.put(ItemContract.ItemEntry.COLUMN_ITEM_URI, jsonObject.getString(ItemContract.ItemEntry.COLUMN_ITEM_URI));
            getContentResolver().insert(ItemContract.ItemEntry.CONTENT_URI, values);
        }
        getSupportLoaderManager().restartLoader(ITEM_LOADER, null, this);
        Toast.makeText(this, "Data restored successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                backupData();
            } else {
                Toast.makeText(this, "Write permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == READ_EXTERNAL_STORAGE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                restoreData();
            } else {
                Toast.makeText(this, "Read permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
