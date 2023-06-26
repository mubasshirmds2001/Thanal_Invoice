package com.example.thanal_invoice;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class Splash_Activity extends AppCompatActivity {

    ImageView logo;
    TextView tagLine, footer, logoText;
    Animation scale, fade;

    private static int SPLASH_SCREEN_TIME = 2000;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_splash);

        logo = findViewById(R.id.logo);
        logoText = findViewById(R.id.logo_text);
        tagLine = findViewById(R.id.tagline);
        //

        scale = AnimationUtils.loadAnimation(this, R.anim.scale);
        fade = AnimationUtils.loadAnimation(this, R.anim.fade);

        logo.setAnimation(scale);
        logoText.setAnimation(fade);
        tagLine.setAnimation(fade);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                    startActivity(new Intent(getApplicationContext(),InvoiceActivity.class));
                    finish();
                }
        }, 2000);
        }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        Intent intent = new Intent(Intent.ACTION_MAIN);
//        intent.addCategory(Intent.CATEGORY_HOME);
//        startActivity(intent);
        moveTaskToBack(true);
        finish();
    }
}