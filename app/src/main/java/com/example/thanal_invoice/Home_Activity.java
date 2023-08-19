package com.example.thanal_invoice;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Home_Activity extends AppCompatActivity {
    Button billbtn;
    Button viewbtn;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        billbtn = (Button) findViewById(R.id.Billbtn);


        billbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Home_Activity.this, InvoiceActivity.class);
                startActivity(intent);
            }
        });
    }
    @Override
    public void onBackPressed() {
        finish(); // Finish the current activity
    }
}