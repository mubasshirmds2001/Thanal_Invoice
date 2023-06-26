package com.example.thanal_invoice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
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
    private EditText itemEditText;
    private EditText quantityEditText;
    private EditText rateEditText;
    private Button savePdfButton;
    private ListView itemListView;

    private ArrayList<InvoiceItem> invoiceItems;
    private InvoiceItemAdapter itemAdapter;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> createFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice);

        // Initialize views
        logoImageView = findViewById(R.id.logoImageView);
        totalAmountTextView = findViewById(R.id.totalAmountTextView);
        itemEditText = findViewById(R.id.itemEditText);
        quantityEditText = findViewById(R.id.quantityEditText);
        rateEditText = findViewById(R.id.rateEditText);
        savePdfButton = findViewById(R.id.savePdfButton);
        itemListView = findViewById(R.id.itemListView);

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
                String item = itemEditText.getText().toString();
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
                itemEditText.setText("");
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
                Document document = new Document();

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
                document.add(new Paragraph("Your Business Name", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));

                // Add the invoice items
                for (InvoiceItem item : invoiceItems) {
                    document.add(new Paragraph(item.getItem()));
                    document.add(new Paragraph("Quantity: " + item.getQuantity()));
                    document.add(new Paragraph("Rate: $" + item.getRate()));
                    document.add(new Paragraph("Price: $" + item.getPrice()));
                    document.add(new Paragraph("\n"));
                }

                // Add the total amount
                document.add(new Paragraph("Total Amount: $" + calculateTotalAmount()));

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
        android.graphics.Canvas canvas = page.getCanvas();

        // Add the header (business name and logo)
        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        canvas.drawBitmap(logoBitmap, 50, 50, null);
        canvas.drawText("Your Business Name", 200, 100, null);

        // Add the invoice items
        int y = 200;
        for (InvoiceItem item : invoiceItems) {
            canvas.drawText(item.getItem(), 50, y, null);
            canvas.drawText("Quantity: " + item.getQuantity(), 50, y + 30, null);
            canvas.drawText("Rate: $" + item.getRate(), 50, y + 60, null);
            canvas.drawText("Price: $" + item.getPrice(), 50, y + 90, null);
            y += 120;
        }

        // Add the total amount
        canvas.drawText("Total Amount: $" + calculateTotalAmount(), 50, y, null);

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
