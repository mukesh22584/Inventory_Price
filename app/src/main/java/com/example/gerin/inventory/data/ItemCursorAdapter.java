package com.example.gerin.inventory.data;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.gerin.inventory.R;
import com.example.gerin.inventory.data.ItemContract.ItemEntry;

import java.util.Locale;

public class ItemCursorAdapter extends CursorAdapter {

    public ItemCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    private static class ViewHolder {
        final TextView nameView;
        final TextView quantityView;
        final TextView priceView;

        int nameIdx, qtyIdx, unitIdx, priceIdx, currIdx;

        ViewHolder(View view, Cursor cursor) {
            nameView = view.findViewById(R.id.name);
            quantityView = view.findViewById(R.id.quantity);
            priceView = view.findViewById(R.id.price);

            nameIdx = cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_NAME);
            qtyIdx = cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_QUANTITY);
            unitIdx = cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_UNIT);
            priceIdx = cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_PRICE);
            currIdx = cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_CURRENCY);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        ViewHolder holder = new ViewHolder(view, cursor);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();

        String name = cursor.getString(holder.nameIdx);
        int quantity = cursor.getInt(holder.qtyIdx);
        String unit = cursor.getString(holder.unitIdx);
        double price = cursor.getDouble(holder.priceIdx);
        String currency = cursor.getString(holder.currIdx);

        if (unit == null) unit = "";
        if (currency == null || currency.isEmpty()) currency = "₹";

        holder.nameView.setText(name);
        
        holder.quantityView.setText(String.format(Locale.getDefault(), "%d %s", quantity, unit).trim());
        holder.priceView.setText(String.format(Locale.getDefault(), "%s %.2f", currency, price));
    }
}
