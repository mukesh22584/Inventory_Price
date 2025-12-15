package com.example.gerin.inventory;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
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
import com.example.gerin.inventory.Search.RecyclerTouchListener;
import com.example.gerin.inventory.Search.SearchResult;
import com.example.gerin.inventory.data.ItemContract;
import com.example.gerin.inventory.data.ItemCursorAdapter;
import com.example.gerin.inventory.data.ItemDbHelper;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

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
        flag1 = 0;
        Log.e("catalog", "onStart");

    }

    @Override
    protected void onPause() {
        super.onPause();

        flag1 = 1;
        Log.e("catalog", "onPause");
        materialSearchBar.clearSuggestions();
        materialSearchBar.disableSearch();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Create a new instance of the database for access to the searchbar
        database = new ItemDbHelper(this);

        // Create the search bar
        materialSearchBar = (MaterialSearchBar) findViewById(R.id.search_bar1);
        materialSearchBar.setCardViewElevation(0);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        customSuggestionsAdapter = new CustomSuggestionsAdapter(inflater);

        if (flag1 == 0) {
            Log.e("catalog", "tried to set adapter");
            loadSearchResultList();
            customSuggestionsAdapter.setSuggestions(searchResultList);
            materialSearchBar.setCustomSuggestionAdapter(customSuggestionsAdapter);
//          ^---- this line causes problems when starting a new intent
        }

        // Add flags to determine when to stop loading search results
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                materialSearchBar.disableSearch();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (flag1 == 0) {
                    List<SearchResult> newSuggestions = loadNewSearchResultList();
                    customSuggestionsAdapter.setSuggestions(newSuggestions);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
//                if (flag1 == 0) {
//                    if (!materialSearchBar.isSuggestionsVisible()) {
//                        if (s.toString() != null && !s.toString().isEmpty()) {
//                            materialSearchBar.enableSearch();
//                        }
//                    }
//                }
            }
        });
        // Useless
        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
//                if (!enabled)
//                    adapter = new SearchAdapter(getBaseContext(), database.getResult());
////                    recyclerView.setAdapter(null);i
//                if(enabled) {
//                    materialSearchBar.enableSearch();
//                    materialSearchBar.setCustomSuggestionAdapter(customSuggestionsAdapter);
//                }
//                else {
//                    materialSearchBar.clearSuggestions();
//                    materialSearchBar.disableSearch();
//                }
//                if (flag1 == 0) {
//                    if (enabled)
//                        materialSearchBar.showSuggestionsList();
//                }

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {

                List<SearchResult> testResult1 = loadNewSearchResultList();
                if(testResult1.isEmpty()) {
                    Toast.makeText(getBaseContext(), "No Results Found",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                SearchResult testResult2 = testResult1.get(0);
                String testResult4 = testResult2.getName();
                int testResult3 = testResult2.getId();

                if(text.toString().toLowerCase().equals(testResult4.toLowerCase())){
//                    Toast.makeText(getBaseContext(), "Search Success!",
//                            Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(CatalogActivity.this, ItemActivity.class);

                    Uri currentPetUri = ContentUris.withAppendedId(ItemContract.ItemEntry.CONTENT_URI, testResult3);
                    // Set the URI on the data field of the intent
                    intent.setData(currentPetUri);

                    flag1 = 1;
                    materialSearchBar.clearSuggestions();
                    // probably dont need this line
                    materialSearchBar.disableSearch();

                    startActivity(intent);

                }
                else{
                    Toast.makeText(getBaseContext(), "No Results Found",
                            Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onButtonClicked(int buttonCode) {

//                recyclerView.setAdapter(null);

                switch (buttonCode) {
                    case MaterialSearchBar.BUTTON_NAVIGATION:
                        Log.e("catalog", "button clicked");
                        materialSearchBar.clearSuggestions();
                        materialSearchBar.disableSearch();
                        break;
                    case MaterialSearchBar.BUTTON_SPEECH:
                        break;
                    default:
                        break;

                }
            }
        });
        // Doesn't work for custom adapters
        materialSearchBar.setSuggstionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {

            @Override
            public void OnItemClickListener(int position, View v) {
                Log.e("catalog", "on item click");
                Log.e("on item click", String.valueOf(position));
            }

            @Override
            public void OnItemDeleteListener(int position, View v) {
                Log.e("catalog", "on item delete");
            }
        });

        // On click method for suggestions
        RecyclerView searchrv = findViewById(getResources().getIdentifier("mt_recycler", "id", getPackageName()));
        searchrv.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), searchrv, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {

                //id works with original search list but not new???
//                int _id = searchResultList.get(position)._id;
// TODO: 2018-07-13 clean up code here since we dont need _id anymore now that we have the searchResult object
                // need testResult1 to work for some reason???
                List<SearchResult> testResult1 = loadNewSearchResultList();
                SearchResult testResult2 = testResult1.get(position);
                int testResult3 = testResult2.getId();

//                Log.e("catalog", "position = " + String.valueOf(position));
//                Log.e("catalog", "_id = " + String.valueOf(_id));
//                Log.e("catalog", "testResult3 = " + String.valueOf(testResult3));


                Intent intent = new Intent(CatalogActivity.this, ItemActivity.class);

                // Form the content URI that represents the specific pet that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link PetEntry#CONTENT_URI}.
                // For example, the URI would be "content://com.example.android.pets/pets/2"
                // if the pet with ID 2 was clicked on.
                Uri currentPetUri = ContentUris.withAppendedId(ItemContract.ItemEntry.CONTENT_URI, testResult3);
                // Set the URI on the data field of the intent
                intent.setData(currentPetUri);

                Log.e("catalog", "list item click");
                flag1 = 1;
                materialSearchBar.clearSuggestions();
                // probably dont need this line
                materialSearchBar.disableSearch();

                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, int position) {
                TextView tv = (TextView) view.findViewById(R.id.search_text);
                materialSearchBar.setText(String.valueOf(tv.getText()));
            }
        }));

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

                // Form the content URI that represents the specific pet that was clicked on,
                // by appending the "id" (passed as input to this method) onto the
                // {@link PetEntry#CONTENT_URI}.
                // For example, the URI would be "content://com.example.android.pets/pets/2"
                // if the pet with ID 2 was clicked on.
                Uri currentPetUri = ContentUris.withAppendedId(ItemContract.ItemEntry.CONTENT_URI, id);
                // Set the URI on the data field of the intent
                intent.setData(currentPetUri);

                Log.e("catalog", "list item click");
                flag1 = 1;
                materialSearchBar.clearSuggestions();
                materialSearchBar.disableSearch();

                startActivity(intent);
            }
        });

        // Kick off the loader
        getLoaderManager().initLoader(ITEM_LOADER, null, this);

    }

    private void startSearch(String s) {

//        adapter = new SearchAdapter(this, database.getResultNames(s));
    }

    private void loadSearchResultList() {
        searchResultList = database.getResult();
    }

    private List<SearchResult> loadNewSearchResultList() {
        List<SearchResult> newSuggestions = new ArrayList<>();
        List<Integer> newSuggestions_id = new ArrayList<Integer>(10);
        loadSearchResultList();
        int i = 0;
        for (SearchResult searchResult : searchResultList) {
            if (searchResult.getName().toLowerCase().contains(materialSearchBar.getText().toLowerCase())) {
                newSuggestions.add(searchResult);
                newSuggestions_id.add(searchResult.getId());
//                MySuggestions.moreresults[i] = searchResult.getId();
                i++;

//                MySuggestions.newSuggestions_id.add(1);
                Log.d("_id", String.valueOf(searchResult.getId()));
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
                getLoaderManager().restartLoader(ITEM_LOADER, bundle, CatalogActivity.this);
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
