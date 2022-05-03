package com.nachex.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.nachex.MainActivity;
import com.nachex.util.PrefManager;
import com.nachex.R;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(PrefManager.getString(SplashActivity.this, PrefManager.IS_USER_LOGGED_IN).equals("true")){
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                } else{
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("EXIT", "false"));
                    finish();
                }
            }
        }, 2000);
    }
}