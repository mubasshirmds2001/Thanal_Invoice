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
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
            quantityEditText.requestFocus();

            double grandTotal = 0.0;

            // Update the totalAmount variable by adding the price of the current item
            grandTotal += Double.parseDouble(Price);

            // Set the totalAmount in the TextView
            totalAmountTextView.setText("Grand Total :" + String.valueOf(grandTotal));

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
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

                // Notify the adapter of the data set change
                itemAdapter.notifyDataSetChanged();
            }
        });
    }

    private void generatePDF() throws IOException {
        String pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File file = new File(pdfPath, "Invoice.pdf");
        OutputStream outputStream = new FileOutputStream(file);


        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdfdocument = new PdfDocument(writer);
        Document document1 = new Document(pdfdocument);

        pdfdocument.setDefaultPageSize(PageSize.A4);
        document1.setMargins(0,0,0,0);

        Drawable drawable = getDrawable(R.drawable.header);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
        byte[] bitmapdata = stream.toByteArray();

        ImageData imageData = ImageDataFactory.create(bitmapdata);
        Image image = new Image(imageData);

        Table headerTable = createHeaderTable();

        // Set the position of the header
        pdfdocument.addEventHandler(PdfDocumentEvent.START_PAGE, event -> {
            float x = document1.getLeftMargin();
            float y = document1.getTopMargin();
            headerTable.setFixedPosition(x, y, PageSize.A4.getWidth());
        });

        // Add the headerTable to each page
        pdfdocument.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
            document1.add(headerTable);
        });

        // Add table header
        Table table = new Table(new float[]{1, 3, 2, 2, 2});
        table.addHeaderCell(new Cell().add(new Paragraph("Sl.No.").setTextAlignment(TextAlignment.CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Item Name").setTextAlignment(TextAlignment.CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Quantity").setTextAlignment(TextAlignment.CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Rate").setTextAlignment(TextAlignment.CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Total Amount").setTextAlignment(TextAlignment.CENTER)));

        // Add items to table
        int slNo = 1;
        for (InvoiceItem item : invoiceItems) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(slNo++)).setTextAlignment(TextAlignment.CENTER)));
            table.addCell(new Cell().add(new Paragraph(item.getItem()).setTextAlignment(TextAlignment.CENTER)));
            table.addCell(new Cell().add(new Paragraph(item.getQty()).setTextAlignment(TextAlignment.CENTER)));
            table.addCell(new Cell().add(new Paragraph(item.getRate()).setTextAlignment(TextAlignment.CENTER)));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(item.getPrice())).setTextAlignment(TextAlignment.CENTER)));
        }
        document1.add(table);

        // Add final total
        String grandTotalText = totalAmountTextView.getText().toString();
        document1.add(new Paragraph(grandTotalText));

        document1.close();
        outputStream.close();

        Toast.makeText(this, "PDF generated successfully", Toast.LENGTH_SHORT).show();
    }

    private Table createHeaderTable() {
        Table table = new Table(2);
        table.setWidth(100);

        // Create the first cell and add the first paragraph
        Cell clientNameCell = new Cell();
        Paragraph clientNameParagraph = new Paragraph("Client Name: ")
                .setBold()
                .setFontSize(14)
                .setTextAlignment(TextAlignment.LEFT);
        clientNameCell.add(clientNameParagraph);
        table.addCell(clientNameCell);

        // Create the second cell and add the second paragraph
        Cell clientNameValueCell = new Cell();
        Paragraph clientNameValue = new Paragraph(clientEditText.getText().toString())
                .setBold()
                .setFontSize(14)
                .setTextAlignment(TextAlignment.LEFT);
        clientNameValueCell.add(clientNameValue);
        table.addCell(clientNameValueCell);

        return table;
    }

    @Override
    public void onBackPressed() {
        // Go to the HomeActivity
        Intent intent = new Intent(this, Home_Activity.class);
        startActivity(intent);
        finish(); // Finish the current activity
    }
}
