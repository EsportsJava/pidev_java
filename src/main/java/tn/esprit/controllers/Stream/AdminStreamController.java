package tn.esprit.controllers.Stream;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import tn.esprit.entities.Stream;
import tn.esprit.services.ServiceStream;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class AdminStreamController {

    @FXML private WebView webView;
    @FXML private Label statusLabel;

    @FXML private Label likeCounterLabel;
    @FXML private Label loveCounterLabel;
    @FXML private Label hahaCounterLabel;
    @FXML private Label wowCounterLabel;

    @FXML private ListView<String> commentList;
    @FXML private TextField commentField;

    private WebSocketClient client;
    private final ServiceStream serviceStream = new ServiceStream();

    private int likeCount = 0;
    private int loveCount = 0;
    private int hahaCount = 0;
    private int wowCount = 0;

    @FXML
    public void initialize() {

        serviceStream.ensureStreamExists();

        loadStream();
        connectWebSocket();
        setupUI();

        commentField.setOnAction(e -> sendComment());
    }

    // ================= STREAM =================
    private void loadStream() {

        Stream stream = serviceStream.getActiveStream();

        if (stream == null || stream.getUrl() == null) {
            statusLabel.setText("❌ No Stream");
            return;
        }

        String url = stream.getUrl();

        String html =
                "<html><head>" +
                        "<script src='https://cdn.jsdelivr.net/npm/hls.js'></script>" +
                        "</head><body style='margin:0;background:black;'>" +
                        "<video id='v' controls autoplay style='width:100%;height:100%'></video>" +
                        "<script>" +
                        "var v=document.getElementById('v');" +
                        "var src='" + url + "';" +
                        "if(Hls.isSupported()){" +
                        "var h=new Hls();h.loadSource(src);h.attachMedia(v);" +
                        "}else{v.src=src;}" +
                        "</script></body></html>";

        webView.getEngine().loadContent(html);
        statusLabel.setText("🔴 LIVE");
    }

    // ================= WS =================
    private void connectWebSocket() {

        try {
            client = new WebSocketClient(new URI("ws://localhost:8887")) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Admin connected");
                }

                @Override
                public void onMessage(String message) {

                    Platform.runLater(() -> {

                        String[] p = message.split("\\|", 4);

                        // ================= REACTIONS =================
                        if (p.length == 4 && p[0].equals("REACTION")) {

                            String user = p[2];
                            String type = p[3];

                            switch (type) {

                                case "LIKE" -> {
                                    likeCount++;
                                    likeCounterLabel.setText("👍 " + likeCount);
                                }

                                case "LOVE" -> {
                                    loveCount++;
                                    loveCounterLabel.setText("❤️ " + loveCount);
                                }

                                case "HAHA" -> {
                                    hahaCount++;
                                    hahaCounterLabel.setText("😂 " + hahaCount);
                                }

                                case "WOW" -> {
                                    wowCount++;
                                    wowCounterLabel.setText("😮 " + wowCount);
                                }
                            }

                            commentList.getItems().add("💥 " + user + " reacted " + type);
                        }

                        // ================= COMMENTS =================
                        else if (p.length == 4 && p[0].equals("COMMENT")) {

                            String user = p[2];
                            String text = p[3];

                            commentList.getItems().add(user + " : " + text);
                        }

                        commentList.scrollTo(commentList.getItems().size() - 1);
                    });
                }

                @Override public void onClose(int code, String reason, boolean remote) {}
                @Override public void onError(Exception ex) { ex.printStackTrace(); }
            };

            client.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= COMMENT =================
    @FXML
    private void sendComment() {

        String text = commentField.getText();
        if (text == null || text.isEmpty()) return;

        client.send("COMMENT|ADMIN|Admin|" + text);
        commentField.clear();
    }

    // ================= UI =================
    private void setupUI() {

        likeCounterLabel.setText("👍 0");
        loveCounterLabel.setText("❤️ 0");
        hahaCounterLabel.setText("😂 0");
        wowCounterLabel.setText("😮 0");
    }
}