package com.example.socket;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 讀取啟動參數
        String role = getIntent().getStringExtra("ROLE");

        if ("SERVER".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, ServerActivity.class));
            finish();
        } else if ("CLIENT".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, ClientActivity.class));
            finish();
        }
    }
}