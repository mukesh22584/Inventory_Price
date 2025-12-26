package com.example.gerin.inventory;

import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.*;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
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
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

import java.util.ArrayList;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ITEM_LOADER = 0;

    private ItemCursorAdapter mCursorAdapter;
    private MaterialSearchBar materialSearchBar;
    private CustomSuggestionsAdapter customSuggestionsAdapter;
    private ItemDbHelper database;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
        
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

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        int savedMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
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

            materialSearchBar.setLastSuggestions(new ArrayList<>());
            materialSearchBar.setCardViewElevation(2); 

            materialSearchBar.addTextChangeListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    handler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> {
                        if (customSuggestionsAdapter != null) {
                            customSuggestionsAdapter.getFilter().filter(s.toString());
                        }

                        Bundle bundle = new Bundle();
                        bundle.putString("filterName", s.toString().trim());
                        LoaderManager.getInstance(CatalogActivity.this).restartLoader(ITEM_LOADER, bundle, CatalogActivity.this);
                    };
                    handler.postDelayed(searchRunnable, 300);
                }
            });

            materialSearchBar.setSuggestionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
                @Override
                public void OnItemClickListener(int position, View v) {
                    SearchResult result = (SearchResult) customSuggestionsAdapter.getSuggestions().get(position);
                    materialSearchBar.clearSuggestions();
                    materialSearchBar.closeSearch();
                    launchItemActivity(result.getId());
                }

                @Override public void OnItemDeleteListener(int position, View v) {}
            });

            materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
                @Override
                public void onSearchStateChanged(boolean enabled) {
                    if (!enabled) {
                        LoaderManager.getInstance(CatalogActivity.this).restartLoader(ITEM_LOADER, null, CatalogActivity.this);
                    } else if (customSuggestionsAdapter != null) {
                        materialSearchBar.setLastSuggestions(new ArrayList<>());
                        customSuggestionsAdapter.getFilter().filter("");
                    }
                }

                @Override
                public void onSearchConfirmed(CharSequence text) {
                    Bundle bundle = new Bundle();
                    bundle.putString("filterName", text.toString().trim());
                    LoaderManager.getInstance(CatalogActivity.this).restartLoader(ITEM_LOADER, bundle, CatalogActivity.this);
                    
                    materialSearchBar.hideSuggestionsList(); 
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
        if (id == R.id.action_sort_by) {
            showSortByDialog();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        String[] projection = {
                ItemEntry._ID, 
                ItemEntry.COLUMN_ITEM_NAME, 
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_PRICE, 
                ItemEntry.COLUMN_ITEM_UNIT,
                ItemEntry.COLUMN_ITEM_CURRENCY, 
                ItemEntry.COLUMN_ITEM_IMAGE,
                ItemEntry.COLUMN_ITEM_DESCRIPTION, 
                ItemEntry.COLUMN_ITEM_URI,
                ItemEntry.COLUMN_ITEM_SIZE, 
                ItemEntry.COLUMN_ITEM_SIZE_UNIT                
        };

        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;

        if (bundle != null) {
            if (bundle.containsKey("sortOrder")) {
                sortOrder = bundle.getString("sortOrder");
            }
            if (bundle.containsKey("filterName")) {
                String name = bundle.getString("filterName");
                if (name != null && !name.trim().isEmpty()) {
                    selection = ItemEntry.COLUMN_ITEM_NAME + " LIKE ?";
                    selectionArgs = new String[]{"%" + name.trim() + "%"};
                }
            }
        }

        return new CursorLoader(this, ItemEntry.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
    }

    @Override public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) { 
        if (mCursorAdapter != null) {
            mCursorAdapter.swapCursor(data);
            
            if (data != null && data.getCount() == 0 && materialSearchBar != null && materialSearchBar.isSearchOpened() && !materialSearchBar.getText().isEmpty()) {
                showToast("No items found matching: " + materialSearchBar.getText());
            }
        }
    }
    
    @Override public void onLoaderReset(@NonNull Loader<Cursor> loader) { 
        if (mCursorAdapter != null) mCursorAdapter.swapCursor(null); 
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
