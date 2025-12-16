package com.example.gerin.inventory;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.loader.app.LoaderManager;
import android.content.ContentUris;
import androidx.loader.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.loader.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gerin.inventory.Search.CustomSuggestionsAdapter;
import com.example.gerin.inventory.Search.SearchResult;
import com.example.gerin.inventory.data.ItemContract;
import com.example.gerin.inventory.data.ItemCursorAdapter;
import com.example.gerin.inventory.data.ItemDbHelper;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.List;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private ItemCursorAdapter mCursorAdapter;

    /**
     * Identifier for the item data loader
     */
    private static final int ITEM_LOADER = 0;

    /**
     * Variables for the search bar
     */
    MaterialSearchBar materialSearchBar;
    CustomSuggestionsAdapter customSuggestionsAdapter;
    ItemDbHelper database;
    List<SearchResult> searchResultList = new ArrayList<>();
    public static int flag1 = 0;

    @Override
    protected void onStart() {
        super.onStart();
        if (materialSearchBar.isSuggestionsVisible()) {
            loadSearchResultList();
            materialSearchBar.setLastSuggestions(searchResultList);
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
        materialSearchBar = (MaterialSearchBar) findViewById(R.id.search_bar1);
        materialSearchBar.setCardViewElevation(0);
        loadSearchResultList();
        materialSearchBar.setLastSuggestions(searchResultList);

        // Add flags to determine when to stop loading search results
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                materialSearchBar.disableSearch();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<SearchResult> newSuggestions = loadNewSearchResultList();
                materialSearchBar.setLastSuggestions(newSuggestions);

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
                if (newSuggestions.size() > 0) {
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
        ListView itemListView = (ListView) findViewById(R.id.catalog_list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        itemListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of pet data in the Cursor.
        // There is no pet data yet (until the loader finishes) so pass in null for the Cursor.
        mCursorAdapter = new ItemCursorAdapter(this, null, 0);
        itemListView.setAdapter(mCursorAdapter);


        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.catalog_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Setup the item click listener
        itemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create new intent to go to {@link EditorActivity}
                Intent intent = new Intent(CatalogActivity.this, ItemActivity.class);

                Uri currentPetUri = ContentUris.withAppendedId(ItemContract.ItemEntry.CONTENT_URI, id);
                intent.setData(currentPetUri);

                startActivity(intent);
            }
        });

        // Kick off the loader
        LoaderManager.getInstance(this).initLoader(ITEM_LOADER, null, this);

    }

    private void loadSearchResultList() {
        searchResultList = database.getResult();
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
        builder.setSingleChoiceItems(sortOptions, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bundle bundle = new Bundle();
                bundle.putString("sortOrder", sortOrderOptions[which]);
                LoaderManager.getInstance(CatalogActivity.this).restartLoader(ITEM_LOADER, bundle, CatalogActivity.this);
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
    }

}
