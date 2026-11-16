package com.example.socket;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerActivity extends AppCompatActivity {

    private EditText editServerName;
    private Button btnServerConnect;
    private ServerSocket serverSocket;
    private boolean isServerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        // 設定 Header 標題
        TextView headerTitle = findViewById(R.id.header);
        headerTitle.setText("Server");

        editServerName = findViewById(R.id.editServerName);
        btnServerConnect = findViewById(R.id.btnServerConnect);

        btnServerConnect.setOnClickListener(v -> {
            String name = editServerName.getText().toString().trim();
            if (!name.isEmpty()) {
                // 1. 啟動 Socket Server
                SocketManager.getInstance().startServer(name, 8080);

                // 2. 執行頁面跳轉 (這段必須要有！)
                Intent intent = new Intent(ServerActivity.this, ChatActivity.class);
                intent.putExtra("NAME", name);
                intent.putExtra("ROLE", "SERVER");
                startActivity(intent);
            }
        });
    }

    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8080); // 預設 Port 為 8080
                isServerRunning = true;
                runOnUiThread(() -> Toast.makeText(ServerActivity.this, "Server 啟動成功 (Port: 8080)", Toast.LENGTH_SHORT).show());

                while (isServerRunning) {
                    Socket clientSocket = serverSocket.accept(); // 等待 Client 連線
                    runOnUiThread(() -> Toast.makeText(ServerActivity.this, "Client 已連線: " + clientSocket.getInetAddress(), Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}