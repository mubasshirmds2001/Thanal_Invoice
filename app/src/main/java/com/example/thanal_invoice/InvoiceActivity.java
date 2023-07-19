package com.example.thanal_invoice;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class InvoiceActivity extends AppCompatActivity {

    private EditText clientEditText;
    private Spinner spinner_item;
    private EditText quantityEditText;
    private EditText rateEditText;
    private TextView totalAmountTextView;
    private TextView priceTextView;

    private Button addItemButton;
    private Button clearItemButton;
    private Button saveListButton;

    private ListView itemListView;

    private ArrayList<InvoiceItem> invoiceItems;
    private InvoiceItemAdapter itemAdapter;

    DatabaseReference databaseReference;

    FirebaseDatabase firebaseDatabase;

    public double Amount;

    @SuppressLint({"MissingInflatedId", "DefaultLocale"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice);

        // Initialize views
        clientEditText = findViewById(R.id.clientEditText);
        spinner_item = findViewById(R.id.spinner_item);
        quantityEditText = findViewById(R.id.quantityEditText);
        rateEditText = findViewById(R.id.rateEditText);
        totalAmountTextView = findViewById(R.id.totalAmountTextView);
        addItemButton = findViewById(R.id.addItemButton);
        clearItemButton = findViewById(R.id.clearItemButton);
        saveListButton = findViewById(R.id.saveListButton);
        itemListView = findViewById(R.id.itemListView);
        priceTextView = findViewById(R.id.priceTextView);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.item_list, android.R.layout.simple_spinner_dropdown_item);
        spinner_item.setAdapter(adapter);
        String defValue = "Select Items";
        int position = adapter.getPosition(defValue);  // get position of value
        spinner_item.setSelection(position);  // set selected item by position

        // Initialize the invoice items list and adapter
        invoiceItems = new ArrayList<>();
        itemAdapter = new InvoiceItemAdapter(this, invoiceItems);
        itemListView.setAdapter(itemAdapter);

        // Add item button click listener
        addItemButton.setOnClickListener(v -> {
            // Validate client name
            String clientName = clientEditText.getText().toString().trim();
            if (clientName.isEmpty()) {
                clientEditText.setError("Client name is required.");
                return;
            }

            // Validate spinner selection
            String spinnerItem = spinner_item.getSelectedItem().toString();
            if (spinnerItem.isEmpty()) {
                Toast.makeText(InvoiceActivity.this, "Please select an item.", Toast.LENGTH_SHORT).show();
                return;
            }

            String Quantity = quantityEditText.getText().toString().trim();
            if (Quantity.isEmpty()) {
                quantityEditText.setError("Quantity is required.");
                return;
            }

            String Rate = rateEditText.getText().toString().trim();
            if (Rate.isEmpty()) {
                rateEditText.setError("Rate is required.");
                return;
            }

            int quantity = Integer.parseInt(quantityEditText.getText().toString());
            int rate = Integer.parseInt(rateEditText.getText().toString());
            String Price = String.valueOf(quantity * rate);

            // Create a new invoice item
            InvoiceItem invoiceItem = new InvoiceItem(clientName,spinnerItem, Quantity, Rate,Price);


            // Add the item to the list
            invoiceItems.add(invoiceItem);

            // Update the ListView
            itemAdapter.notifyDataSetChanged();

            // Clear input fields
            quantityEditText.setText("");
            rateEditText.setText("");
            quantityEditText.requestFocus();

            double grandTotal = 0.0;

            // Update the totalAmount variable by adding the price of the current item
            grandTotal += Double.parseDouble(Price);

            // Set the totalAmount in the TextView
            totalAmountTextView.setText("Grand Total :"+String.valueOf(grandTotal));

        });

        clearItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear the invoiceItems list
                invoiceItems.clear();

                // Notify the adapter of the data set change
                itemAdapter.notifyDataSetChanged();
            }
        });

        saveListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Check if at least one item is present in the ListView
                if (invoiceItems.size() > 0) {
                    // At least one item is present, proceed with adding data to the database
                    firebaseDatabase = FirebaseDatabase.getInstance();
                    databaseReference = firebaseDatabase.getReference("Bills");

                    // Generate a random ID for the list
                    String BillNumber = databaseReference.push().getKey();

                    // Store the entire list in the database under the list ID
                    databaseReference.child(BillNumber).setValue(invoiceItems)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getApplicationContext(), "Data Inserted", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Failed to insert data", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                } else {
                    // No items are present, show an error message or take appropriate action
                    Toast.makeText(getApplicationContext(), "Please add at least one item.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Go to the HomeActivity
        Intent intent = new Intent(this, Home_Activity.class);
        startActivity(intent);
        finish(); // Finish the current activity
    }
}
