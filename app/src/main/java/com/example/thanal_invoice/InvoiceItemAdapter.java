package com.example.thanal_invoice;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class InvoiceItemAdapter extends ArrayAdapter<InvoiceItem> {
    public InvoiceItemAdapter(Context context, ArrayList<InvoiceItem> items) {
        super(context, 0, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_invoice, parent, false);
        }

        // Get the item at the current position
        InvoiceItem item = getItem(position);

        // Bind the item data to the views
        TextView itemTextView = convertView.findViewById(R.id.itemTextView);
        TextView quantityTextView = convertView.findViewById(R.id.quantityTextView);
        TextView rateTextView = convertView.findViewById(R.id.rateTextView);
        TextView priceTextView = convertView.findViewById(R.id.priceTextView);

        itemTextView.setText(item.getItem());
        quantityTextView.setText(String.valueOf(item.getQty()));
        rateTextView.setText(String.valueOf(item.getRate()));
        priceTextView.setText(String.format("%.2f", item.getPrice()));

        return convertView;
    }
}
