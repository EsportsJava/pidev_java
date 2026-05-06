package tn.esprit.websocket;

public class WebSocketLauncher {

    public static void main(String[] args) {

        CommentWebSocketServer server = new CommentWebSocketServer(8887);
        server.start();

        System.out.println("🌐 WS running: ws://localhost:8887");
    }
}