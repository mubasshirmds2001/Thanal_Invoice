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
        logoImageView = findViewById(R.id.logoImageView);
        totalAmountTextView = findViewById(R.id.totalAmountTextView);
        itemSpinner = findViewById(R.id.spinner_item);
        quantityEditText = findViewById(R.id.quantityEditText);
        rateEditText = findViewById(R.id.rateEditText);
        savePdfButton = findViewById(R.id.savePdfButton);
        itemListView = findViewById(R.id.itemListView);
        clientEditText = findViewById(R.id.clientEditText);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.item_list, android.R.layout.simple_spinner_item);
        itemSpinner.setAdapter(adapter);

        // Set the logo image
        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        logoImageView.setImageBitmap(logoBitmap);

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
                totalAmountTextView.setText(String.format("Total Amount: Rs%.2f", totalAmount));

                // Clear input fields
                quantityEditText.setText("");
                rateEditText.setText("");
            }
        });

        // Request permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        createPdfFile();
                    } else {
                        Toast.makeText(this, "Permission denied. Cannot save PDF.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Create file launcher
        createFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent intent = result.getData();
                        if (intent != null) {
                            Uri uri = intent.getData();
                            if (uri != null) {
                                savePdfDocument(uri);
                            }
                        }
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
            savePdfLegacy();
        }
    }

    private void savePdfDocument(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            if (pfd != null) {
                FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

                // Create a new document
                com.itextpdf.text.Document document = new com.itextpdf.text.Document();

                // Set page size and margins
                document.setPageSize(PageSize.A4);
                document.setMargins(50, 50, 50, 50);

                // Create a PdfWriter instance
                PdfWriter pdfWriter = PdfWriter.getInstance(document, fileOutputStream);

                // Open the document
                document.open();

                // Add the header (business name and logo)
                Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                logoBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                com.itextpdf.text.Image logoImage = com.itextpdf.text.Image.getInstance(byteArray);
                logoImage.scaleToFit(100, 100);
                document.add(logoImage);
                document.add(new com.itextpdf.text.Paragraph("THANAL EVENTS SARALIKATTE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));

                // Add the client name
                String clientName = clientEditText.getText().toString();
                document.add(new com.itextpdf.text.Paragraph("Client Name: " + clientName));

                // Add the spinner item
                String spinnerItem = itemSpinner.getSelectedItem().toString();
                document.add(new com.itextpdf.text.Paragraph("Item: " + spinnerItem));

                // Add the invoice items
                for (InvoiceItem item : invoiceItems) {
                    document.add(new com.itextpdf.text.Paragraph(item.getItem()));
                    document.add(new com.itextpdf.text.Paragraph("Quantity: " + item.getQuantity()));
                    document.add(new com.itextpdf.text.Paragraph("Rate: Rs " + item.getRate()));
                    document.add(new com.itextpdf.text.Paragraph("Price: Rs " + item.getPrice()));
                    document.add(new com.itextpdf.text.Paragraph("\n"));
                }

                // Add the total amount
                document.add(new com.itextpdf.text.Paragraph("Total Amount: Rs " + calculateTotalAmount()));

                // Close the document
                document.close();
                pdfWriter.close();

                Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePdfLegacy() {
        // Create a new document
        PdfDocument document = new PdfDocument();

        // Create a page info
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();

        // Start a page
        PdfDocument.Page page = document.startPage(pageInfo);

        // Create a canvas for drawing
        Canvas canvas = page.getCanvas();

        // Add the header (business name and logo)
        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        int logoWidth = logoBitmap.getWidth();
        int logoHeight = logoBitmap.getHeight();
        int centerX = pageInfo.getPageWidth() / 2;
        int logoLeft = centerX - (logoWidth / 2);
        int logoTop = 50;
        canvas.drawBitmap(logoBitmap, logoLeft, logoTop, null);

        // Set font style for the header
        Paint headerPaint = new Paint();
        headerPaint.setTextSize(16f);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Add business name
        String businessName = "THANAL EVENTS";
        float businessNameWidth = headerPaint.measureText(businessName);
        int businessNameLeft = centerX - (int) (businessNameWidth / 2);
        int businessNameTop = logoTop + logoHeight + 30;
        canvas.drawText(businessName, businessNameLeft, businessNameTop, headerPaint);

        // Set font style for address and contact details
        Paint detailsPaint = new Paint();
        detailsPaint.setTextSize(12f);

        // Add business address
        String businessAddress = "SARALIKATTE, THEKKAR POST & VILLAGE, BELTHANGADY TALUK";
        float businessAddressWidth = detailsPaint.measureText(businessAddress);
        int businessAddressLeft = centerX - (int) (businessAddressWidth / 2);
        int businessAddressTop = businessNameTop + 20;
        canvas.drawText(businessAddress, businessAddressLeft, businessAddressTop, detailsPaint);

        // Add business contact
        String businessContact = "+91 9880478873";
        float businessContactWidth = detailsPaint.measureText(businessContact);
        int businessContactLeft = centerX - (int) (businessContactWidth / 2);
        int businessContactTop = businessAddressTop + 20;
        canvas.drawText(businessContact, businessContactLeft, businessContactTop, detailsPaint);

        // Add the client name
        String clientName = clientEditText.getText().toString();
        canvas.drawText("Client Name: " + clientName, 50, businessContactTop + 30, null);

        // Set font style for the table
        Paint tablePaint = new Paint();
        tablePaint.setTextSize(14f);

        // Define table column widths
        int serialNumberColumn = 50;
        int itemColumn = 200;
        int quantityColumn = 350;
        int rateColumn = 450;
        int priceColumn = 550;

        // Draw table headers
        canvas.drawText("Serial No.", serialNumberColumn, businessContactTop + 80, tablePaint);
        canvas.drawText("Item", itemColumn, businessContactTop + 80, tablePaint);
        canvas.drawText("Quantity", quantityColumn, businessContactTop + 80, tablePaint);
        canvas.drawText("Rate", rateColumn, businessContactTop + 80, tablePaint);
        canvas.drawText("Price", priceColumn, businessContactTop + 80, tablePaint);

        // Add the invoice items
        int y = businessContactTop + 120;
        int serialNumber = 1;
        for (InvoiceItem item : invoiceItems) {
            // Draw serial number
            canvas.drawText(String.valueOf(serialNumber), serialNumberColumn, y, tablePaint);

            // Draw item
            canvas.drawText(item.getItem(), itemColumn, y, tablePaint);

            // Draw quantity
            canvas.drawText(String.valueOf(item.getQuantity()), quantityColumn, y, tablePaint);

            // Draw rate
            canvas.drawText("Rs " + item.getRate(), rateColumn, y, tablePaint);

            // Draw price
            canvas.drawText("Rs " + item.getPrice(), priceColumn, y, tablePaint);

            y += 30;
            serialNumber++;
        }

        // Add the total amount
        String totalAmount = "Total Amount: Rs " + calculateTotalAmount();
        Paint totalPaint = new Paint();
        totalPaint.setTextSize(14f);
        totalPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(totalAmount, 50, y + 30, totalPaint);

        // Finish the page
        document.finishPage(page);

        // Save the document
        String fileName = "invoice.pdf";
        File filePath = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
        try {
            OutputStream outputStream = new FileOutputStream(filePath);
            document.writeTo(outputStream);
            document.close();
            outputStream.close();
            Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save PDF.", Toast.LENGTH_SHORT).show();
        }
    }

}
