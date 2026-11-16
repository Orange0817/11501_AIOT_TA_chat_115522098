package com.example.socket;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.net.Socket;

public class ClientActivity extends AppCompatActivity {

    private EditText editClientName, editClientIp, editClientPort;
    private Button btnClientConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        // 設定 Header 標題
        TextView headerTitle = findViewById(R.id.header);
        headerTitle.setText("Client");

        editClientName = findViewById(R.id.editClientName);
        editClientIp = findViewById(R.id.editClientIp);
        editClientPort = findViewById(R.id.editClientPort);
        btnClientConnect = findViewById(R.id.btnClientConnect);

        btnClientConnect.setOnClickListener(v -> {
            String name = editClientName.getText().toString().trim();
            String ip = editClientIp.getText().toString().trim();
            String portStr = editClientPort.getText().toString().trim();

            if (!name.isEmpty() && !ip.isEmpty() && !portStr.isEmpty()) {
                int port = Integer.parseInt(portStr);

                // 1. 建立 Socket 連線
                SocketManager.getInstance().connectClient(name, ip, port);

                // 2. 跳轉到聊天室 (必須包含這段)
                Intent intent = new Intent(ClientActivity.this, ChatActivity.class);
                intent.putExtra("NAME", name);
                intent.putExtra("ROLE", "CLIENT");
                startActivity(intent);
            } else {
                Toast.makeText(this, "請填寫完整資訊", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connectToServer() {
        String name = editClientName.getText().toString().trim();
        String ip = editClientIp.getText().toString().trim();
        String portStr = editClientPort.getText().toString().trim();

        if (name.isEmpty() || ip.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, "請完整填寫所有欄位", Toast.LENGTH_SHORT).show();
            return;
        }

        int port = Integer.parseInt(portStr);

        // Socket 連線屬網路操作，必須放在背景執行緒 (Thread)
        new Thread(() -> {
            try {
                Socket socket = new Socket(ip, port);
                runOnUiThread(() -> Toast.makeText(ClientActivity.this, "連線成功！", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ClientActivity.this, "連線失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}