package com.example.thanal_invoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class InvoiceActivity extends AppCompatActivity {

    private EditText clientEditText;
    private Spinner spinner_item;
    private EditText quantityEditText;
    private EditText rateEditText;
    private TextView totalAmountTextView;

    public Button addItemButton;
    public Button clearItemButton;
    public Button saveListButton;

    private ListView itemListView;

    private ArrayList<InvoiceItem> invoiceItems;
    private InvoiceItemAdapter itemAdapter;

    public double Amount;

    private static final int PERMISSION_REQUEST_CODE = 123;

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
        TextView priceTextView = findViewById(R.id.priceTextView);

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
                clientEditText.requestFocus();
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
            InvoiceItem invoiceItem = new InvoiceItem(clientName, spinnerItem, Quantity, Rate, Price);


            // Add the item to the list
            invoiceItems.add(invoiceItem);

            // Update the ListView
            itemAdapter.notifyDataSetChanged();

            // Clear input fields
            quantityEditText.setText("");
            rateEditText.setText("");
            spinner_item.requestFocus();

            double grandTotal = 0.0;

            for (InvoiceItem item : invoiceItems) {
                grandTotal += item.getPrice();
            }
            totalAmountTextView.setText("Grand Total: " + String.valueOf(grandTotal));
        });

        saveListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    generatePDF();
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        clearItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear the invoiceItems list
                invoiceItems.clear();
                totalAmountTextView.setText("");

                // Notify the adapter of the data set change
                itemAdapter.notifyDataSetChanged();
            }
        });
    }

    private void generatePDF() throws IOException {
        int templateResourceId = R.raw.custompdf;
        InputStream inputStream = getResources().openRawResource(templateResourceId);
        PdfReader templateReader = new PdfReader(inputStream);

        String pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File file = new File(pdfPath, "Invoice.pdf");
        OutputStream outputStream = new FileOutputStream(file);


        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdfdocument = new PdfDocument(templateReader,writer);
        Document document1 = new Document(pdfdocument);

        String clientName = clientEditText.getText().toString();
        Paragraph labelParagraph = new Paragraph("Client Name: "+clientName)
                .setBold().setFontSize(18).setMarginTop(130).setFontColor(new DeviceRgb(139, 0, 0));

        document1.add(labelParagraph);
        document1.add(new Paragraph("\n")); // Blank space

        // Add table header
        Table table = new Table(new float[]{1, 3, 2, 2, 2}).setMarginTop(3);
        Color headerBackgroundColor = new DeviceRgb(0, 0, 255);
        table.addHeaderCell(new Cell().add(new Paragraph("Sl.No.").setFontColor(headerBackgroundColor).setTextAlignment(TextAlignment.CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Item Name").setFontColor(headerBackgroundColor).setTextAlignment(TextAlignment.CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Quantity").setFontColor(headerBackgroundColor).setTextAlignment(TextAlignment.CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Rate").setFontColor(headerBackgroundColor).setTextAlignment(TextAlignment.CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Total Amount").setFontColor(headerBackgroundColor).setTextAlignment(TextAlignment.CENTER)));

        double grandTotal = 0.0;
        // Add items to table
        int slNo = 1;
        for (InvoiceItem item : invoiceItems) {
            table.addCell(createCell(String.valueOf(slNo++), 50, 20));
            table.addCell(createCell(item.getItem(), 150, 20));
            table.addCell(createCell(item.getQty(), 80, 20));
            table.addCell(createCell(item.getRate(), 80, 20));
            table.addCell(createCell(String.valueOf(item.getPrice()), 100, 20));

            grandTotal += item.getPrice();
        }
        document1.add(table);

        Paragraph grandTotalParagraph = new Paragraph("Grand Total:  " + grandTotal)
                .setBold().setFontSize(16).setMarginLeft(340)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginTop(20); // Adjust as needed
        document1.add(grandTotalParagraph);

        document1.close();
        outputStream.close();

        Toast.makeText(this, "PDF generated successfully", Toast.LENGTH_SHORT).show();
    }

    private Cell createCell(String content, float width, float height) {
        return new Cell()
                .add(new Paragraph(content)
                        .setMarginTop(10).setFontColor(new DeviceRgb(255, 0, 0))
                        .setHeight(height)
                        .setWidth(width)
                        .setTextAlignment(TextAlignment.CENTER));
    }

    @Override
    public void onBackPressed() {
        // Go to the HomeActivity
        Intent intent = new Intent(this, Home_Activity.class);
        startActivity(intent);
        finish(); // Finish the current activity
    }
}
