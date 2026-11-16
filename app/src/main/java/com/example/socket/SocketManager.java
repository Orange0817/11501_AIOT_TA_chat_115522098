package com.example.socket;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SocketManager {
    private static SocketManager instance;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    private MessageListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isServer = false;

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    public static synchronized SocketManager getInstance() {
        if (instance == null) instance = new SocketManager();
        return instance;
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    // 啟動 Server
    public void startServer(String serverName, int port) {
        this.isServer = true;
        new Thread(() -> {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                serverSocket = new ServerSocket(port);
                
                String ip = getLocalIpAddress();
                // 延遲一下通知，確保 ChatActivity 的 listener 已經設定好
                mainHandler.postDelayed(() -> 
                    notifyMessage(serverName + " started. (" + ip + ":" + port + ")"), 500);

                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket, serverName);
                    synchronized (clients) {
                        clients.add(handler);
                    }
                    new Thread(handler).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Client 連線
    public void connectClient(String clientName, String ip, int port) {
        this.isServer = false;
        new Thread(() -> {
            try {
                clientSocket = new Socket(ip, port);
                
                // 延遲發送 JOIN，確保 Server 的 ClientHandler 已經準備好，且本地 Listener 已掛載
                Thread.sleep(500);
                
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println("JOIN:" + clientName); // 傳送加入請求

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String msg;
                while ((msg = in.readLine()) != null) {
                    notifyMessage(msg);
                }
            } catch (Exception e) {
                notifyMessage("Connection failed: " + e.getMessage());
            }
        }).start();
    }

    // 廣播訊息給所有 Client (Server 專用)
    public void broadcast(String msg) {
        // 1. 通知 UI 顯示
        notifyMessage(msg);

        // 2. 網路寫入必須在背景執行緒中進行，避免 NetworkOnMainThreadException
        new Thread(() -> {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.send(msg);
                }
            }
        }).start();
    }

    // Client 發送訊息給 Server
    public void sendClientMessage(String msg) {
        new Thread(() -> {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println(msg);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    public void leave() {
        new Thread(() -> {
            try {
                if (isServer && serverSocket != null) serverSocket.close();
                if (clientSocket != null) clientSocket.close();
                synchronized (clients) {
                    clients.clear();
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void notifyMessage(String msg) {
        mainHandler.post(() -> {
            if (listener != null) listener.onMessageReceived(msg);
        });
    }

    // 獲取區域網路 IP 的輔助方法
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.getHostAddress().contains(".")) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return "127.0.0.1";
    }

    // 處理單個 Client 連線的 Handler
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final String serverName;
        private PrintWriter out;
        private String clientName = "Unknown";

        public ClientHandler(Socket socket, String serverName) {
            this.socket = socket;
            this.serverName = serverName;
        }

        public void send(String msg) {
            if (out != null) out.println(msg);
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                String clientIpPort = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("JOIN:")) {
                        clientName = line.substring(5);
                        // 僅 Server 端（本地）顯示 Connected 訊息
                        notifyMessage(clientName + " (" + clientIpPort + ") Connected.");
                        // 廣播 Welcome 訊息給所有人（包含 Client 才會看到）
                        broadcast(serverName + ": Welcome " + clientName + "!");
                    } else {
                        broadcast(clientName + ": " + line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Client 離線處理 (不論是正常退出或是異常斷開)
                synchronized (clients) {
                    clients.remove(this);
                }
                broadcast(serverName + ": " + clientName + " has left.");
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }
}