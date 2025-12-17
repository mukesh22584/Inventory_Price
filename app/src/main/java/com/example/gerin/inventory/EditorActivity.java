package com.example.gerin.inventory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import androidx.loader.app.LoaderManager;
import android.content.ContentValues;
import androidx.loader.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.loader.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.gerin.inventory.data.ItemContract;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

// TODO: 2018-07-09 if user clicks save twice two copies are saved
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the item data loader
     */
    private static final int EXISTING_ITEM_LOADER = 0;

    /**
     * Content URI for the existing item (null if it's a new item)
     */
    private Uri mCurrentItemUri;

    /**
     * EditText field to enter the item's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the item's quantity
     */
    private EditText mQuantityEditText;

    /**
     * Spinner for quantity unit
     */
    private Spinner mUnitSpinner;

    /**
     * Unit of the item. The possible values are:
     * Pcs., Kgs., Ltr., Mtr.
     */
    private String mUnit;

    /**
     * EditText field to enter the item's price
     */
    private EditText mPriceEditText;

    /**
     * Spinner for currency
     */
    private Spinner mCurrencySpinner;

    /**
     * Currency of the item.
     */
    private String mCurrency;

    /**
     * EditText field for tag 1
     */
    private EditText mTag1EditText;

    /**
     * EditText field for tag 2
     */
    private EditText mTag2EditText;

    /**
     * EditText field for tag 3
     */
    private EditText mTag3EditText;

    /**
     * EditText field to enter the item's description
     */
    private EditText mDescriptionEditText;

    /**
     * ImageView to show product image
     */
    private ImageView mItemImageView;

    /**
     * Bitmap of item's image
     */
    public Bitmap mItemBitmap;

    /**
     * Camera FAB
     */
    public FloatingActionButton fab;

    /**
     * Boolean flag that keeps track of whether the item has been edited (true) or not (false)
     */
    private boolean mItemHasChanged = false;

    /**
     * ID for accessing image from gallery
     */
    private static final int GALLERY_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 3;


    /**
     * Maximum size for an image file that can be stored in the database
     */
    private static final int MAX_MB = 5000000;

    /**
     * URI of selected image
     */
    private Uri selectedImage = null;
    String mCurrentPhotoPath;

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener mTouchListener = (view, motionEvent) -> {
        mItemHasChanged = true;
        return false;
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new item or editing an existing one.
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        // If the intent DOES NOT contain a content URI, then we know that we are
        // creating a new item.
        if (mCurrentItemUri == null) {
            // This is a new pet, so change the app bar to say "Add a Pet"
            setTitle(getString(R.string.editor_activity_title_new_item));
        } else {
            // Otherwise this is an existing pet, so change app bar to say "Edit Pet"
            setTitle(getString(R.string.editor_activity_title_edit_item));

            // Initialize a loader to read the item data from the database
            // and display the current values in the editor
            LoaderManager.getInstance(this).initLoader(EXISTING_ITEM_LOADER, null, this);
        }


        mNameEditText = findViewById(R.id.edit_item_name);
        mQuantityEditText = findViewById(R.id.edit_item_quantity);
        mUnitSpinner = findViewById(R.id.spinner_unit);
        mPriceEditText = findViewById(R.id.edit_item_price);
        mCurrencySpinner = findViewById(R.id.spinner_currency);
        mDescriptionEditText = findViewById(R.id.edit_item_description);
        mItemImageView = findViewById(R.id.edit_item_image);
        mTag1EditText = findViewById(R.id.edit_item_tag1);
        mTag2EditText = findViewById(R.id.edit_item_tag2);
        mTag3EditText = findViewById(R.id.edit_item_tag3);
        fab = findViewById(R.id.floatingActionButton);

        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mUnitSpinner.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mCurrencySpinner.setOnTouchListener(mTouchListener);
        mDescriptionEditText.setOnTouchListener(mTouchListener);
        mTag1EditText.setOnTouchListener(mTouchListener);
        mTag2EditText.setOnTouchListener(mTouchListener);
        mTag3EditText.setOnTouchListener(mTouchListener);
        fab.setOnTouchListener(mTouchListener);

        mItemBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.image_prompt)).getBitmap();

        mItemImageView.setOnClickListener(v -> {
            Dialog d = new Dialog(EditorActivity.this);
            d.setContentView(R.layout.custom_dialog);
            ImageView image_full = d.findViewById(R.id.image_full);
            if (mItemBitmap != null)
                image_full.setImageBitmap(mItemBitmap);
            d.show();
        });

        setupSpinner();

    }

    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter<CharSequence> unitSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.unit_options, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> currencySpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.currency_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        unitSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        currencySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mUnitSpinner.setAdapter(unitSpinnerAdapter);
        mCurrencySpinner.setAdapter(currencySpinnerAdapter);

        // Set the integer mSelected to the constant values
        mUnitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mUnit = (String) parent.getItemAtPosition(position);
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mUnit = "Pcs."; // Default to Pcs.
            }
        });

        mCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrency = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCurrency = "₹";
            }
        });
    }

    private void saveItem() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String descriptionString = mDescriptionEditText.getText().toString().trim();
        String tag1String = mTag1EditText.getText().toString().trim();
        String tag2String = mTag2EditText.getText().toString().trim();
        String tag3String = mTag3EditText.getText().toString().trim();
        String imageUri;
        if (selectedImage == null)
            imageUri = "null";
        else
            imageUri = selectedImage.toString();     // may cause error since default is null

        int quantityInteger = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantityInteger = Integer.parseInt(quantityString);
        }

        double priceDouble = 0;
        if (!TextUtils.isEmpty(priceString)) {
            priceDouble = Double.parseDouble(priceString);
        }

        // TODO: 2018-07-08 check for blank inputs in edit mode
        //        // Check if this is supposed to be a new pet
        //        // and check if all the fields in the editor are blank
        //        if (mCurrentItemUri == null &&
        //                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(breedString) &&
        //                TextUtils.isEmpty(weightString) && mGender == PetEntry.GENDER_UNKNOWN) {
        //            // Since no fields were modified, we can return early without creating a new pet.
        //            // No need to create ContentValues and no need to do any ContentProvider operations.
        //            return;
        //        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mItemBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] photo = baos.toByteArray();

        Log.e("save method", "converted to byte array");

        // Create a ContentValues object where column names are the keys,
        // and pet attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_NAME, nameString);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY, quantityInteger);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_UNIT, mUnit);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_PRICE, priceDouble);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_CURRENCY, mCurrency);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_DESCRIPTION, descriptionString);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG1, tag1String);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG2, tag2String);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG3, tag3String);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_IMAGE, photo);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_URI, imageUri);

        // if URI is null, then we are adding a new item
        if (mCurrentItemUri == null) {
            // This is a NEW item, so insert a new item into the provider,
            // returning the content URI for the new item.
            Uri newUri = getContentResolver().insert(ItemContract.ItemEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING item, so update the item with content URI: mCurrentItemUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentPetUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void deleteItem() {
        // Only perform the delete if this is an existing item.
        if (mCurrentItemUri != null) {
            // Call the ContentResolver to delete the item at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentItemUri
            // content URI already identifies the item that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete_entry);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        int itemId = item.getItemId();
        if (itemId == R.id.action_save) {
            saveItem();
            finish();
            return true;
        } else if (itemId == R.id.action_delete_entry) {
            showDeleteConfirmationDialog();
            return true;
        } else if (itemId == android.R.id.home) {
            if (mItemHasChanged)
                showUnsavedChangesDialog();
            else
                finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Since the editor shows all item attributes, define a projection that contains
        // all columns from the inventory table
        String[] projection = {
                ItemContract.ItemEntry._ID,
                ItemContract.ItemEntry.COLUMN_ITEM_NAME,
                ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemContract.ItemEntry.COLUMN_ITEM_UNIT,
                ItemContract.ItemEntry.COLUMN_ITEM_PRICE,
                ItemContract.ItemEntry.COLUMN_ITEM_CURRENCY,
                ItemContract.ItemEntry.COLUMN_ITEM_DESCRIPTION,
                ItemContract.ItemEntry.COLUMN_ITEM_TAG1,
                ItemContract.ItemEntry.COLUMN_ITEM_TAG2,
                ItemContract.ItemEntry.COLUMN_ITEM_TAG3,
                ItemContract.ItemEntry.COLUMN_ITEM_IMAGE,
                ItemContract.ItemEntry.COLUMN_ITEM_URI};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,         // Query the content URI for the current item
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (data == null || data.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (data.moveToFirst()) {
            // Find the columns of pet attributes that we're interested in
            int nameColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_NAME);
            int quantityColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY);
            int unitColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_UNIT);
            int priceColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_PRICE);
            int currencyColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_CURRENCY);
            int descriptionColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_DESCRIPTION);
            int tag1ColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_TAG1);
            int tag2ColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_TAG2);
            int tag3ColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_TAG3);
            int imageColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_IMAGE);
            int uriColumnIndex = data.getColumnIndex(ItemContract.ItemEntry.COLUMN_ITEM_URI);


            // Extract out the value from the Cursor for the given column index
            String name = data.getString(nameColumnIndex);
            int quantity = data.getInt(quantityColumnIndex);
            String unit = data.getString(unitColumnIndex);
            double price = data.getDouble(priceColumnIndex);
            String currency = data.getString(currencyColumnIndex);
            String description = data.getString(descriptionColumnIndex);
            String tag1 = data.getString(tag1ColumnIndex);
            String tag2 = data.getString(tag2ColumnIndex);
            String tag3 = data.getString(tag3ColumnIndex);
            byte[] photo = data.getBlob(imageColumnIndex);
            String imageURI = data.getString(uriColumnIndex);

            ByteArrayInputStream imageStream = new ByteArrayInputStream(photo);
            Bitmap theImage = BitmapFactory.decodeStream(imageStream);


            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mQuantityEditText.setText(String.format("%d", quantity));

            ArrayAdapter<CharSequence> unitAdapter = (ArrayAdapter<CharSequence>) mUnitSpinner.getAdapter();
            if (unit != null) {
                int spinnerPosition = unitAdapter.getPosition(unit);
                mUnitSpinner.setSelection(spinnerPosition);
            }

            ArrayAdapter<CharSequence> currencyAdapter = (ArrayAdapter<CharSequence>) mCurrencySpinner.getAdapter();
            if (currency != null) {
                int spinnerPosition = currencyAdapter.getPosition(currency);
                mCurrencySpinner.setSelection(spinnerPosition);
            }

            DecimalFormat formatter = new DecimalFormat("#0.00");
            mPriceEditText.setText(formatter.format(price));
            mDescriptionEditText.setText(description);
            mTag1EditText.setText(tag1);
            mTag2EditText.setText(tag2);
            mTag3EditText.setText(tag3);
            mItemImageView.setImageBitmap(theImage);
            mItemBitmap = theImage;
            if (imageURI.equals("null"))
                selectedImage = null;
            else
                selectedImage = Uri.parse(imageURI);

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        Bitmap tempItemBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.image_prompt)).getBitmap();

        mNameEditText.setText("");
        mQuantityEditText.setText("");
        mUnitSpinner.setSelection(0);
        mPriceEditText.setText("");
        mCurrencySpinner.setSelection(0);
        mDescriptionEditText.setText("");
        mTag1EditText.setText("");
        mTag2EditText.setText("");
        mTag3EditText.setText("");
        mItemImageView.setImageBitmap(tempItemBitmap);
        selectedImage = null;
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, (dialog, id) -> {
            deleteItem();
        });
        builder.setNegativeButton(R.string.cancel, (dialog, id) -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showUnsavedChangesDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.return_dialog_msg);
            public void onClick(DialogInterface dialog, int id) -> {
                // User clicked the "Discard" button
                finish();
        });
        builder.setNegativeButton(R.string.edit, (dialog, id) -> {
                // User clicked the "Cancel" button, so dismiss the dialog and continue editing
                if (dialog != null) {
                    dialog.dismiss();
                }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void insertImage(View view){
        final CharSequence[] items = {"Take Photo", "Choose from Library", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(EditorActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, (dialog, item) -> {
            if (items[item].equals("Take Photo")) {
                dispatchTakePictureIntent();
            } else if (items[item].equals("Choose from Library")) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
            } else if (items[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY_REQUEST) {
                if (data != null) {
                selectedImage = data.getData();
                Log.e("editor activity", selectedImage.toString());
                try {
                    mItemBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    int i = mItemBitmap.getAllocationByteCount();
                    // if less than 5MB set the image
                    if (i < MAX_MB) {
                        mItemImageView.setImageBitmap(mItemBitmap);
                        Log.e("Editor Activity", "successfully converted image");
                    }
                    else {
                        mItemBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.image_prompt)).getBitmap();
                        selectedImage = null;
                        Log.e("Editor Activity", "image too large");
                        Toast.makeText(this, "Image too large", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("Editor Activity", String.valueOf(i));
                } catch (IOException e) {
                    Log.e("onActivityResult", "Some exception " + e);
                }
                }
            } else if (requestCode == CAMERA_REQUEST) {
                try {
                    mItemBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    if (mItemBitmap.getAllocationByteCount() > MAX_MB) {
                        int currentWidth = mItemBitmap.getWidth();
                        int currentHeight = mItemBitmap.getHeight();
                        int newWidth = (int) (currentWidth * 0.5);
                        int newHeight = (int) (currentHeight * 0.5);
                        mItemBitmap = Bitmap.createScaledBitmap(mItemBitmap, newWidth, newHeight, true);
                    }
                    mItemImageView.setImageBitmap(mItemBitmap);
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(selectedImage);
                    this.sendBroadcast(mediaScanIntent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("fileError", ex.getMessage());
            }
            if (photoFile != null) {
                selectedImage = FileProvider.getUriForFile(this,
                        "com.example.gerin.inventory.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImage);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    private void dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            launchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private void shareImage(){
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, selectedImage);
        shareIntent.setType("image/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share image using"));
    }

}
