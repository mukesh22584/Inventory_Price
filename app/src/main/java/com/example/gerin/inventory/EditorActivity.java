package com.example.gerin.inventory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import androidx.loader.app.LoaderManager;
import android.content.ContentValues;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import com.google.android.material.textfield.TextInputLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.gerin.inventory.data.ItemContract;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_ITEM_LOADER = 0;
    private Uri mCurrentItemUri;
    private EditText mNameEditText, mQuantityEditText, mPriceEditText, mDescriptionEditText;
    private EditText mTag1EditText, mTag2EditText, mTag3EditText;
    private TextInputLayout mQuantityTextInputLayout, mPriceTextInputLayout;
    private ImageView mItemImageView;
    private boolean mItemHasChanged = false;

    private String mUnit = "Pcs.";
    private String mCurrency = "₹";

    private static final int GALLERY_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 3;

    private static final int MAX_IMAGE_DIMENSION = 1024; 
    private Uri selectedImage = null;

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener mTouchListener = (view, motionEvent) -> {
        mItemHasChanged = true;
        return false;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nestedScrollView), (v, insets) -> {
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mPriceTextInputLayout = findViewById(R.id.price_input_layout);
        mQuantityTextInputLayout = findViewById(R.id.quantity_input_layout);

        mCurrentItemUri = getIntent().getData();

        mNameEditText = findViewById(R.id.edit_item_name);
        mQuantityEditText = findViewById(R.id.edit_item_quantity);
        mPriceEditText = findViewById(R.id.edit_item_price);
        mDescriptionEditText = findViewById(R.id.edit_item_description);
        mItemImageView = findViewById(R.id.edit_item_image);
        mTag1EditText = findViewById(R.id.edit_item_tag1);
        mTag2EditText = findViewById(R.id.edit_item_tag2);
        mTag3EditText = findViewById(R.id.edit_item_tag3);

        if (mCurrentItemUri == null) {
            setTitle("Add Item");
            mQuantityTextInputLayout.setSuffixText(mUnit);
            mPriceTextInputLayout.setSuffixText(mCurrency);
        } else {
            setTitle("Edit Item");
            LoaderManager.getInstance(this).initLoader(EXISTING_ITEM_LOADER, null, this);
        }

        setupListeners();
        setupPopupMenus();
    }

    private void setupListeners() {
        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mDescriptionEditText.setOnTouchListener(mTouchListener);
        mTag1EditText.setOnTouchListener(mTouchListener);
        mTag2EditText.setOnTouchListener(mTouchListener);
        mTag3EditText.setOnTouchListener(mTouchListener);

        mItemImageView.setOnClickListener(v -> {
            Dialog d = new Dialog(EditorActivity.this);
            d.setContentView(R.layout.custom_dialog);
            ImageView image_full = d.findViewById(R.id.image_full);
            image_full.setImageDrawable(mItemImageView.getDrawable());
            d.show();
        });
    }

    private void setupPopupMenus() {
        mQuantityTextInputLayout.setEndIconOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.unit_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                mUnit = item.getTitle().toString();
                mQuantityTextInputLayout.setSuffixText(mUnit);
                mItemHasChanged = true;
                return true;
            });
            popup.show();
        });

        mPriceTextInputLayout.setEndIconOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.currency_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                mCurrency = item.getTitle().toString();
                mPriceTextInputLayout.setSuffixText(mCurrency);
                mItemHasChanged = true;
                return true;
            });
            popup.show();
        });
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(this, mCurrentItemUri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data == null || !data.moveToFirst()) return;

        mNameEditText.setText(data.getString(data.getColumnIndexOrThrow(ItemContract.ItemEntry.COLUMN_ITEM_NAME)));
        mQuantityEditText.setText(String.valueOf(data.getInt(data.getColumnIndexOrThrow(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY))));
        mPriceEditText.setText(String.valueOf(data.getDouble(data.getColumnIndexOrThrow(ItemContract.ItemEntry.COLUMN_ITEM_PRICE))));
        mDescriptionEditText.setText(data.getString(data.getColumnIndexOrThrow(ItemContract.ItemEntry.COLUMN_ITEM_DESCRIPTION)));
        mTag1EditText.setText(data.getString(data.getColumnIndexOrThrow(ItemContract.ItemEntry.COLUMN_ITEM_TAG1)));
        mTag2EditText.setText(data.getString(data.getColumnIndexOrThrow(ItemContract.ItemEntry.COLUMN_ITEM_TAG2)));
        mTag3EditText.setText(data.getString(data.getColumnIndexOrThrow(ItemContract.ItemEntry.COLUMN_ITEM_TAG3)));

        mUnit = data.getString(data.getColumnIndexOrThrow(ItemContract.ItemEntry.COLUMN_ITEM_UNIT));
        mCurrency = data.getString(data.getColumnIndexOrThrow(ItemContract.ItemEntry.COLUMN_ITEM_CURRENCY));
        
        mQuantityTextInputLayout.setSuffixText(mUnit);
        mPriceTextInputLayout.setSuffixText(mCurrency);

        byte[] photo = data.getBlob(data.getColumnIndexOrThrow(ItemContract.ItemEntry.COLUMN_ITEM_IMAGE));
        if (photo != null && photo.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.length);
            mItemImageView.setImageBitmap(bitmap);
        }
    }

    private void saveItem() {
        Bitmap bitmapToSave;
        if (mItemImageView.getDrawable() instanceof BitmapDrawable) {
            bitmapToSave = ((BitmapDrawable) mItemImageView.getDrawable()).getBitmap();
        } else {
            bitmapToSave = BitmapFactory.decodeResource(getResources(), R.drawable.image_prompt);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap resized = resizeBitmap(bitmapToSave, MAX_IMAGE_DIMENSION);
        
        resized.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] photoBlob = baos.toByteArray();

        ContentValues values = new ContentValues();
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_NAME, mNameEditText.getText().toString().trim());
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_QUANTITY, getIntFromEditText(mQuantityEditText));
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_PRICE, getDoubleFromEditText(mPriceEditText));
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_DESCRIPTION, mDescriptionEditText.getText().toString().trim());
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG1, mTag1EditText.getText().toString().trim());
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG2, mTag2EditText.getText().toString().trim());
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_TAG3, mTag3EditText.getText().toString().trim());
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_IMAGE, photoBlob);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_URI, (selectedImage != null) ? selectedImage.toString() : "null");
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_UNIT, mUnit);
        values.put(ItemContract.ItemEntry.COLUMN_ITEM_CURRENCY, mCurrency);

        if (mCurrentItemUri == null) {
            getContentResolver().insert(ItemContract.ItemEntry.CONTENT_URI, values);
        } else {
            getContentResolver().update(mCurrentItemUri, values, null, null);
        }
    }

    private int getIntFromEditText(EditText et) {
        String s = et.getText().toString().trim();
        return TextUtils.isEmpty(s) ? 0 : Integer.parseInt(s);
    }

    private double getDoubleFromEditText(EditText et) {
        String s = et.getText().toString().trim();
        return TextUtils.isEmpty(s) ? 0.0 : Double.parseDouble(s);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            mItemHasChanged = true;
            
            if (requestCode == GALLERY_REQUEST && data != null) {
                selectedImage = data.getData();
            }
            
            if (selectedImage != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    mItemImageView.setImageBitmap(resizeBitmap(bitmap, MAX_IMAGE_DIMENSION));
                } catch (IOException e) {
                    Log.e("EditorActivity", "Error setting image", e);
                }
            }
        }
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File photoFile = File.createTempFile("IMG_" + timeStamp, ".jpg", storageDir);
            
            selectedImage = FileProvider.getUriForFile(this, "com.example.gerin.inventory.fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImage);
            startActivityForResult(intent, CAMERA_REQUEST);
        } catch (IOException e) {
            Toast.makeText(this, "Camera Error", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeBitmap(Bitmap image, int maxDimension) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= maxDimension && height <= maxDimension) return image;
        float ratio = (float) width / (float) height;
        if (ratio > 1) {
            width = maxDimension;
            height = (int) (width / ratio);
        } else {
            height = maxDimension;
            width = (int) (height * ratio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveItem();
            finish();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            if (!mItemHasChanged) {
                finish();
                return true;
            }

            showUnsavedChangesDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        showUnsavedChangesDialog();
    }

    private void showUnsavedChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        builder.setMessage(R.string.return_dialog_msg);
        builder.setPositiveButton(R.string.discard, (dialog, id) -> finish());
        builder.setNegativeButton(R.string.edit, (dialog, id) -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void insertImage(View view) {
        final CharSequence[] items = {"Take Photo", "Choose Gallery", "Cancel"};
        new AlertDialog.Builder(this).setTitle("Add Photo!").setItems(items, (dialog, i) -> {
            if (i == 0) dispatchTakePictureIntent();
            else if (i == 1) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, GALLERY_REQUEST);
            }
        }).show();
    }

    private void dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else launchCamera();
    }

    @Override public void onLoaderReset(@NonNull Loader<Cursor> loader) {}
}
