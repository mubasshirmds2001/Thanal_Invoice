package com.example.thanal_invoice;

import static com.itextpdf.layout.property.TextAlignment.CENTER;
import static com.itextpdf.layout.property.TextAlignment.RIGHT;

import android.annotation.SuppressLint;
import android.content.Intent;
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

import androidx.appcompat.app.AppCompatActivity;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;

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

        invoiceItems = new ArrayList<>();
        itemAdapter = new InvoiceItemAdapter(this, invoiceItems);
        itemListView.setAdapter(itemAdapter);

        addItemButton.setOnClickListener(v -> {
            // Validate client name
            String clientName = clientEditText.getText().toString().trim();
            if (clientName.isEmpty()) {
                clientEditText.setError("Client name is required.");
                clientEditText.requestFocus();
                return;
            }

            String spinnerItem = spinner_item.getSelectedItem().toString();
            if (spinnerItem.isEmpty() || spinnerItem.equals("Select Items")) {
                Toast.makeText(InvoiceActivity.this, "Please Select Valid Item.", Toast.LENGTH_SHORT).show();
                return;
            }


            String Quantity = quantityEditText.getText().toString().trim();
            if (Quantity.isEmpty()) {
                quantityEditText.setError("Quantity is required.");
                return;
            }

            int qty = Integer.parseInt(Quantity);
            if (qty <= 0 ) {
                quantityEditText.setError("Quantity should be greater than 0");
                return;
            }
            if (qty > 1000) {
                quantityEditText.setError("Quantity cannot be greater than 1000");
                return;
            }

            String Rate = rateEditText.getText().toString().trim();
            if (Rate.isEmpty()) {
                rateEditText.setError("Rate is required.");
                return;
            }

            int rt = Integer.parseInt(Rate);
            if (rt <= 0 ) {
                rateEditText.setError("Rate should be greater than 0");
                return;
            }
            if (rt > 10000) {
                rateEditText.setError("Rate cannot be greater than 10000");
                return;
            }

            int quantity = Integer.parseInt(quantityEditText.getText().toString());
            int rate = Integer.parseInt(rateEditText.getText().toString());
            String Price = String.valueOf(quantity * rate);

            InvoiceItem invoiceItem = new InvoiceItem(clientName, spinnerItem, Quantity, Rate, Price);

            invoiceItems.add(invoiceItem);

            itemAdapter.notifyDataSetChanged();

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
                if (invoiceItems.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "No items to save", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        generatePDF();
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
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

        String cnm = clientEditText.getText().toString();
        String fileName = "Invoice_" + cnm + ".pdf";

        String pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File file = new File(pdfPath, fileName);
        OutputStream outputStream = new FileOutputStream(file);

        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdfdocument = new PdfDocument(templateReader,writer);
        Document document1 = new Document(pdfdocument);

        Paragraph in = new Paragraph("Invoice").setBold().setFontSize(32).setFontColor(new DeviceRgb(255,0,0)).setTextAlignment(CENTER).setMarginTop(90).setUnderline();
        String clientName = clientEditText.getText().toString();
        Paragraph labelParagraph = new Paragraph("Client Name: "+clientName)
                .setBold().setFontSize(18).setMarginTop(-10).setFontColor(new DeviceRgb(139, 0, 0));

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        String formattedDate = String.format("%02d-%02d-%04d", day, month + 1, year);
        Paragraph date = new Paragraph("Date:"+formattedDate).setBold().setFontSize(16).setFontColor(new DeviceRgb(128,0,128)).setTextAlignment(RIGHT).setMarginTop(-20);

        document1.add(in);
        document1.add(date);
        document1.add(labelParagraph);

        Table table = new Table(new float[]{1, 3, 2, 2, 2}).setHorizontalAlignment(HorizontalAlignment.CENTER);
        Color headerBackgroundColor = new DeviceRgb(0, 0, 255);
        table.addHeaderCell(new Cell().add(new Paragraph("Sl.No.").setFontColor(headerBackgroundColor).setTextAlignment(CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Item Name").setFontColor(headerBackgroundColor).setTextAlignment(CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Quantity").setFontColor(headerBackgroundColor).setTextAlignment(CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Rate").setFontColor(headerBackgroundColor).setTextAlignment(CENTER)));
        table.addHeaderCell(new Cell().add(new Paragraph("Total Amount").setFontColor(headerBackgroundColor).setTextAlignment(CENTER)));

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
                .setBold().setFontSize(16).setMarginLeft(340).setFontColor(new DeviceRgb(255,0,0))
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginTop(20); // Adjust as needed
        document1.add(grandTotalParagraph);

        document1.close();
        outputStream.close();

        Toast.makeText(this, "PDF generated successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Home_Activity.class);
        startActivity(intent);
    }

    private Cell createCell(String content, float width, float height) {
        return new Cell()
                .add(new Paragraph(content)
                        .setMarginTop(10).setFontColor(new DeviceRgb(255, 0, 0))
                        .setHeight(height)
                        .setWidth(width)
                        .setTextAlignment(CENTER));
    }

    @Override
    public void onBackPressed() {
        // Go to the HomeActivity
        Intent intent = new Intent(this, Home_Activity.class);
        startActivity(intent);
        finish(); // Finish the current activity
    }
}
