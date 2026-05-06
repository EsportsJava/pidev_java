package tn.esprit.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CommentWebSocketServer extends WebSocketServer {

    private static final Set<WebSocket> clients =
            Collections.synchronizedSet(new HashSet<>());

    public CommentWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        System.out.println("✅ Client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        System.out.println("❌ Client disconnected");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

        System.out.println("📩 " + message);

        synchronized (clients) {
            for (WebSocket client : clients) {
                client.send(message);
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("⚠ WS Error");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("🚀 WebSocket started on port " + getPort());
    }
}