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
    private static final int DATABASE_VERSION = 3;

    public ItemDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the pets table
        String SQL_CREATE_INVENTORY_TABLE =  "CREATE TABLE " + ItemEntry.TABLE_NAME + " ("
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

        Log.e("test", SQL_CREATE_INVENTORY_TABLE);
        db.execSQL(SQL_CREATE_INVENTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + ItemEntry.TABLE_NAME + " ADD COLUMN " + ItemEntry.COLUMN_ITEM_UNIT + " TEXT;");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + ItemEntry.TABLE_NAME + " ADD COLUMN " + ItemEntry.COLUMN_ITEM_CURRENCY + " TEXT;");
        }
    }


    /**
     * get a list of search results for the search bar
     */
    public List<SearchResult> getResults(){

        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
        };

        Cursor cursor = db.query(
                ItemEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );

        List<SearchResult> result = new ArrayList<>();
        int idColumnIndex = cursor.getColumnIndex(ItemEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);

        if (cursor.moveToFirst()){
            while (!cursor.isAfterLast()){
                if (idColumnIndex != -1 && nameColumnIndex != -1) {
                    SearchResult searchResult = new SearchResult(cursor.getInt(idColumnIndex), cursor.getString(nameColumnIndex));
                    result.add(searchResult);
                }
                cursor.moveToNext();
            }
        }
        return result;
    }

    public List<SearchResult> getNewResult(String s){

        SQLiteDatabase db = getReadableDatabase();
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
        };

        String selection = ItemEntry.COLUMN_ITEM_NAME + " LIKE ?";
        String[] selectionArgs = new String[]{"%"+s+"%"};

        Cursor cursor = db.query(
                ItemEntry.TABLE_NAME,   	// The table to query
                projection,            		// The columns to return
                selection,                  	// The columns for the WHERE clause
                selectionArgs,                  // The values for the WHERE clause
                null,                   	// Don't group the rows
                null,                  		// Don't filter by row groups
                null);

        List<SearchResult> result = new ArrayList<>();
        int idColumnIndex = cursor.getColumnIndex(ItemEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);

        if (cursor.moveToFirst()){
            while (!cursor.isAfterLast()){
                 if (idColumnIndex != -1 && nameColumnIndex != -1) {
                    SearchResult searchResult = new SearchResult(cursor.getInt(idColumnIndex), cursor.getString(nameColumnIndex));
                    result.add(searchResult);
                }
                cursor.moveToNext();
            }
        }
        return result;
    }

}
