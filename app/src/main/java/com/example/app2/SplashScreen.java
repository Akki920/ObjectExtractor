package com.example.app2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Intent moveToMainScreen = new Intent(SplashScreen.this, MainActivity.class);
        new Handler().postDelayed(() -> {
            startActivity(moveToMainScreen);
//            overridePendingTransition(R.anim.fade_in, R.anim.fade_out); //for transition
            finish();
        },2000);
    }
}