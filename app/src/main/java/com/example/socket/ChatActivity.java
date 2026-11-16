package com.example.socket;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ChatActivity extends AppCompatActivity implements SocketManager.MessageListener {

    private TextView tvChatLog, headerTitle, tvGreeting;
    private EditText editMessage;
    private String name, role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        name = getIntent().getStringExtra("NAME");
        role = getIntent().getStringExtra("ROLE"); // "SERVER" or "CLIENT"

        headerTitle = findViewById(R.id.header);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvChatLog = findViewById(R.id.tvChatLog);
        editMessage = findViewById(R.id.editMessage);
        Button btnLeave = findViewById(R.id.btnLeave);
        Button btnSend = findViewById(R.id.btnSend);

        headerTitle.setText("SERVER".equals(role) ? "Socket Server" : "Client Server");
        tvGreeting.setText("Hi! " + name);

        SocketManager.getInstance().setListener(this);

        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                if ("SERVER".equals(role)) {
                    SocketManager.getInstance().broadcast(name + ": " + msg);
                } else {
                    SocketManager.getInstance().sendClientMessage(msg);
                }
                editMessage.setText("");
            }
        });

        btnLeave.setOnClickListener(v -> {
            SocketManager.getInstance().leave();
            finish(); // 關閉 ChatActivity，自動回到上一頁 ServerActivity 或 ClientActivity
        });
    }

    @Override
    public void onMessageReceived(String message) {
        tvChatLog.append(message + "\n");
    }
}