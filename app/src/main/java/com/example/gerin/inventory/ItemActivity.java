package com.example.gerin.inventory;

import androidx.core.content.FileProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import androidx.loader.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gerin.inventory.data.ItemContract;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ItemActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private Uri mCurrentItemUri;
    private static final int EXISTING_ITEM_LOADER = 0;
    private Toolbar toolbar;

    TextView itemNameView;
    TextView quantityView;
    TextView priceView;
    TextView descriptionView;
    TextView tag1View;
    TextView tag2View;
    TextView tag3View;
    ImageView imageView;
    private Bitmap mItemBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);

        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        itemNameView = (TextView) findViewById(R.id.item_name_field);
        quantityView = (TextView) findViewById(R.id.item_quantity_field);
        priceView = (TextView) findViewById(R.id.item_price_field);
        descriptionView = (TextView) findViewById(R.id.item_description_field);
        tag1View = (TextView) findViewById(R.id.item_tag1_field);
        tag2View = (TextView) findViewById(R.id.item_tag2_field);
        tag3View = (TextView) findViewById(R.id.item_tag3_field);
        imageView = (ImageView) findViewById(R.id.item_image_field);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.item_fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ItemActivity.this, EditorActivity.class);
                intent.setData(mCurrentItemUri);
                startActivity(intent);
            }
        });

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        }

        LoaderManager.getInstance(this).initLoader(EXISTING_ITEM_LOADER, null, this);

        imageView.setOnClickListener(v -> {
            Intent intent_im = new Intent();
            intent_im.setAction(Intent.ACTION_VIEW);
            Uri imageUri = getImageUri(mItemBitmap);
            intent_im.setDataAndType(imageUri, "image/*");
            intent_im.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent_im);
        });
    }

    private void deleteItem() {
        int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);

        if (rowsDeleted == 0) {
            Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_delete_current_entry) {
            showDeleteConfirmationDialog();
            return true;
        } else if (itemId == R.id.action_edit_current_entry) {
            Intent intent = new Intent(ItemActivity.this, EditorActivity.class);
            intent.setData(mCurrentItemUri);
            startActivity(intent);
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_share) {
            shareImage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
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
                ItemContract.ItemEntry.COLUMN_ITEM_IMAGE};

        return new CursorLoader(this, mCurrentItemUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() < 1) {
            return;
        }

        if (data.moveToFirst()) {
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

            if (photo != null && photo.length > 0) {
                ByteArrayInputStream imageStream = new ByteArrayInputStream(photo);
                mItemBitmap = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(mItemBitmap);
            }

            itemNameView.setText(name);

            quantityView.setText(String.format("%d %s", quantity, (unit == null ? "" : unit)));
            DecimalFormat formatter = new DecimalFormat("#0.00");
            priceView.setText((currency == null ? "" : currency) + formatter.format(price));
            descriptionView.setText(description);

            tag1View.setVisibility(View.GONE);
            tag2View.setVisibility(View.GONE);
            tag3View.setVisibility(View.GONE);

            ArrayList<String> tags = new ArrayList<>();
            if (tag1 != null && !tag1.isEmpty()) {
                tags.add(tag1);
            }
            if (tag2 != null && !tag2.isEmpty()) {
                tags.add(tag2);
            }
            if (tag3 != null && !tag3.isEmpty()) {
                tags.add(tag3);
            }

            if (tags.size() > 0) {
                tag1View.setText(tags.get(0));
                tag1View.setVisibility(View.VISIBLE);
            }
            if (tags.size() > 1) {
                tag2View.setText(tags.get(1));
                tag2View.setVisibility(View.VISIBLE);
            }
            if (tags.size() > 2) {
                tag3View.setText(tags.get(2));
                tag3View.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        itemNameView.setText("");
        quantityView.setText("");
        priceView.setText("");
        descriptionView.setText("");
        tag1View.setText("");
        tag2View.setText("");
        tag3View.setText("");

        Bitmap tempItemBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.image_prompt)).getBitmap();
        imageView.setImageBitmap(tempItemBitmap);
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void shareImage() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        Uri imageUri = getImageUri(mItemBitmap);
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);

        List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivity(Intent.createChooser(shareIntent, "Share image using"));
    }

    private Uri getImageUri(Bitmap bitmap) {
        File imageFile = new File(getCacheDir(), "shared_image.png");
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return FileProvider.getUriForFile(this, "com.example.gerin.inventory.fileprovider", imageFile);
    }
}
