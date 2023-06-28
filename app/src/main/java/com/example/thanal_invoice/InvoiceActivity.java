package com.example.thanal_invoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class InvoiceActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int CREATE_FILE_REQUEST_CODE = 102;

    private ImageView logoImageView;
    private TextView totalAmountTextView;
    private Spinner itemSpinner;
    private EditText quantityEditText;
    private EditText rateEditText;
    private Button savePdfButton;
    private ListView itemListView;

    private EditText clientEditText;

    private ArrayList<InvoiceItem> invoiceItems;
    private InvoiceItemAdapter itemAdapter;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> createFileLauncher;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice);

        // Initialize views
        totalAmountTextView = findViewById(R.id.totalAmountTextView);
        itemSpinner = findViewById(R.id.spinner_item);
        quantityEditText = findViewById(R.id.quantityEditText);
        rateEditText = findViewById(R.id.rateEditText);
        savePdfButton = findViewById(R.id.savePdfButton);
        itemListView = findViewById(R.id.itemListView);
        clientEditText = findViewById(R.id.clientEditText);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.item_list, android.R.layout.simple_spinner_item);
        itemSpinner.setAdapter(adapter);

        Drawable drawable = getResources().getDrawable(R.drawable.header);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        // Initialize the invoice items list and adapter
        invoiceItems = new ArrayList<>();
        itemAdapter = new InvoiceItemAdapter(this, invoiceItems);
        itemListView.setAdapter(itemAdapter);

        // Add item button click listener
        findViewById(R.id.addItemButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate client name
                String clientName = clientEditText.getText().toString().trim();
                if (clientName.isEmpty()) {
                    clientEditText.setError("Client name is required.");
                    return;
                }

                // Validate spinner selection
                String spinnerItem = itemSpinner.getSelectedItem().toString();
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

                String item = itemSpinner.getSelectedItem().toString();
                int quantity = Integer.parseInt(quantityEditText.getText().toString());
                double rate = Double.parseDouble(rateEditText.getText().toString());

                // Create a new invoice item
                InvoiceItem invoiceItem = new InvoiceItem(item, quantity, rate);

                // Add the item to the list
                invoiceItems.add(invoiceItem);

                // Update the ListView
                itemAdapter.notifyDataSetChanged();

                // Calculate and update the total amount
                double totalAmount = calculateTotalAmount();
                totalAmountTextView.setText(String.format("Total Amount: Rs %.2f", totalAmount));

                // Clear input fields
                quantityEditText.setText("");
                rateEditText.setText("");
            }
        });

        createFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intent = result.getData();
                        if (intent != null) {
                            Uri uri = intent.getData();
                            if (uri != null) {
                                savePdfDocument(uri);
                            } else {
                                Toast.makeText(this, "Failed to create PDF. Uri is null.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Failed to create PDF. Intent is null.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        createPdfFile();
                    } else {
                        Toast.makeText(this, "Permission denied. Unable to create PDF.", Toast.LENGTH_SHORT).show();
                    }
                });


        // Save PDF button click listener
        savePdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    checkAndRequestPermission();
                } else {
                    createPdfFile();
                }
            }
        });
    }

    private double calculateTotalAmount() {
        double totalAmount = 0.0;
        for (InvoiceItem item : invoiceItems) {
            totalAmount += item.getPrice();
        }
        return totalAmount;
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                createPdfFile();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                createFileLauncher.launch(intent);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                createPdfFile();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void createPdfFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_TITLE, "invoice.pdf");
            createFileLauncher.launch(intent);
        } else {
            savePdfLegacy(null);
        }
    }

    private void savePdfDocument(Uri uri) {
        new Thread(() -> {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    // Create a new document
                    com.itextpdf.text.Document document = new com.itextpdf.text.Document();

                    // Set page size and margins
                    document.setPageSize(PageSize.A4);
                    document.setMargins(50, 50, 50, 50);

                    // Create a PdfWriter instance
                    PdfWriter pdfWriter = PdfWriter.getInstance(document, outputStream);

                    // Open the document
                    document.open();

                    // Add the header (business name and logo)
                    Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.header);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    logoBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    com.itextpdf.text.Image logoImage = com.itextpdf.text.Image.getInstance(byteArray);

                    // Set the image alignment and width to fit the page
                    logoImage.setAlignment(com.itextpdf.text.Image.ALIGN_TOP);
                    logoImage.scaleAbsoluteWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());

                    // Add the logo image to the document
                    document.add(logoImage);

                    // Add the client name
                    String clientName = clientEditText.getText().toString();
                    document.add(new com.itextpdf.text.Paragraph("Client Name: " + clientName));

                    // Add the invoice table
                    PdfPTable table = new PdfPTable(5);
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(20f);
                    table.setSpacingAfter(20f);

                    // Add table headers
                    table.addCell("Sl. No.");
                    table.addCell("Item Name");
                    table.addCell("Quantity");
                    table.addCell("Rate");
                    table.addCell("Total");

                    // Add table rows for invoice items
                    int slNo = 1;
                    for (InvoiceItem item : invoiceItems) {
                        table.addCell(String.valueOf(slNo));
                        table.addCell(item.getItem());
                        table.addCell(String.valueOf(item.getQuantity()));
                        table.addCell("Rs " + item.getRate());
                        table.addCell("Rs " + item.getPrice());
                        slNo++;
                    }

                    // Add the table to the document
                    document.add(table);

                    // Add the total amount
                    document.add(new com.itextpdf.text.Paragraph("Total Amount: Rs " + calculateTotalAmount()));

                    // Close the document
                    document.close();
                    pdfWriter.close();
                    outputStream.close();

                    runOnUiThread(() -> Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException | DocumentException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void savePdfLegacy(Uri uri) {
        new Thread(() -> {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    // Create a new document
                    PdfDocument document = new PdfDocument();

                    // Create a page info
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();

                    // Start a page
                    PdfDocument.Page page = document.startPage(pageInfo);

                    // Get the canvas of the page
                    Canvas canvas = page.getCanvas();

                    // Add the header (business name and logo)
                    Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.header);
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(logoBitmap, pageInfo.getPageWidth(), logoBitmap.getHeight(), true);
                    canvas.drawBitmap(scaledBitmap, 0, 0, null);

                    // Add the client name
                    String clientName = clientEditText.getText().toString();
                    canvas.drawText("Client Name: " + clientName, 50, logoBitmap.getHeight() + 50, new Paint());

                    // Add the invoice table
                    float y = logoBitmap.getHeight() + 100; // Set the initial y position
                    float lineHeight = 30; // Define the line height

                    // Add table headers
                    canvas.drawText("Sl. No.", 50, y, new Paint());
                    canvas.drawText("Item Name", 150, y, new Paint());
                    canvas.drawText("Quantity", 300, y, new Paint());
                    canvas.drawText("Rate", 400, y, new Paint());
                    canvas.drawText("Total", 500, y, new Paint());

                    // Add table rows for invoice items
                    int slNo = 1;
                    for (InvoiceItem item : invoiceItems) {
                        y += lineHeight;
                        canvas.drawText(String.valueOf(slNo), 50, y, new Paint());
                        canvas.drawText(item.getItem(), 150, y, new Paint());
                        canvas.drawText(String.valueOf(item.getQuantity()), 300, y, new Paint());
                        canvas.drawText("Rs " + item.getRate(), 400, y, new Paint());
                        canvas.drawText("Rs " + item.getPrice(), 500, y, new Paint());
                        slNo++;
                    }

                    // Add the total amount
                    y += lineHeight * 2; // Add extra space after the table
                    canvas.drawText("Total Amount: Rs " + calculateTotalAmount(), 50, y, new Paint());

                    // Finish the page
                    document.finishPage(page);

                    // Write the document content to the output stream
                    document.writeTo(outputStream);

                    // Close the document
                    document.close();
                    outputStream.close();

                    runOnUiThread(() -> Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

}
