package com.example.gerin.inventory.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.gerin.inventory.Search.SearchResult;
import com.example.gerin.inventory.data.ItemContract.ItemEntry;

import java.util.ArrayList;
import java.util.List;

public class ItemDbHelper extends SQLiteOpenHelper{

    /**
     * Name of the database file.
     */
    private static final String DATABASE_NAME = "Inventory.db";
    
    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 5;

    public ItemDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL statement to create the inventory table
        String SQL_CREATE_INVENTORY_TABLE = "CREATE TABLE " + ItemEntry.TABLE_NAME + " ("
                + ItemEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ItemEntry.COLUMN_ITEM_NAME + " TEXT NOT NULL, "
                + ItemEntry.COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                + ItemEntry.COLUMN_ITEM_UNIT + " TEXT, "
                + ItemEntry.COLUMN_ITEM_PRICE + " REAL NOT NULL DEFAULT 0.0, "
                + ItemEntry.COLUMN_ITEM_CURRENCY + " TEXT, "
                + ItemEntry.COLUMN_ITEM_DESCRIPTION + " TEXT, "
                + ItemEntry.COLUMN_ITEM_TAG1 + " TEXT, "
                + ItemEntry.COLUMN_ITEM_TAG2 + " TEXT, "
                + ItemEntry.COLUMN_ITEM_TAG3 + " TEXT, "
                + ItemEntry.COLUMN_ITEM_IMAGE + " BLOB, "
                + ItemEntry.COLUMN_ITEM_URI + " TEXT);";

        db.execSQL(SQL_CREATE_INVENTORY_TABLE);

        db.execSQL("CREATE INDEX idx_item_name ON " + ItemEntry.TABLE_NAME + 
                "(" + ItemEntry.COLUMN_ITEM_NAME + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("ItemDbHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + ItemEntry.TABLE_NAME + " ADD COLUMN " + ItemEntry.COLUMN_ITEM_UNIT + " TEXT;");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + ItemEntry.TABLE_NAME + " ADD COLUMN " + ItemEntry.COLUMN_ITEM_CURRENCY + " TEXT;");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_item_name ON " + ItemEntry.TABLE_NAME + 
                    "(" + ItemEntry.COLUMN_ITEM_NAME + ");");
        }
        
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + ItemEntry.TABLE_NAME + " ADD COLUMN " + ItemEntry.COLUMN_ITEM_IMAGE + " BLOB;");
                db.execSQL("ALTER TABLE " + ItemEntry.TABLE_NAME + " ADD COLUMN " + ItemEntry.COLUMN_ITEM_URI + " TEXT;");
            } catch (Exception e) {
                Log.e("ItemDbHelper", "Columns might already exist: " + e.getMessage());
            }
        }
    }

    public List<SearchResult> getResults() {
        return fetchSearchResults(null, null, null);
    }

    public List<SearchResult> getNewResult(String query) {
        String selection = ItemEntry.COLUMN_ITEM_NAME + " LIKE ?";
        String[] selectionArgs = new String[]{query + "%"};
        return fetchSearchResults(selection, selectionArgs, "10");
    }

    private List<SearchResult> fetchSearchResults(String selection, String[] selectionArgs, String limit) {
        List<SearchResult> resultList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String[] projection = { ItemEntry._ID, ItemEntry.COLUMN_ITEM_NAME };

        try (Cursor cursor = db.query(
                ItemEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                ItemEntry.COLUMN_ITEM_NAME + " ASC",
                limit)) {

            if (cursor != null) {
                int idIdx = cursor.getColumnIndexOrThrow(ItemEntry._ID);
                int nameIdx = cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_NAME);

                while (cursor.moveToNext()) {
                    resultList.add(new SearchResult(
                            cursor.getInt(idIdx),
                            cursor.getString(nameIdx)
                    ));
                }
            }
        } catch (Exception e) {
            Log.e("ItemDbHelper", "Search error", e);
        }
        return resultList;
    }
}
