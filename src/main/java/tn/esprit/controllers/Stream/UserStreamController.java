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

public class UserStreamController {

    @FXML private WebView webView;
    @FXML private Label statusLabel;
    @FXML private ListView<String> commentList;
    @FXML private TextField commentField;

    private WebSocketClient client;
    private final ServiceStream serviceStream = new ServiceStream();

    // ================= INIT =================
    @FXML
    public void initialize() {
        loadStream();
        connectWebSocket();
        setupChatUI();

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
        statusLabel.setText("🔴 LIVE STREAM");
    }

    // ================= WEBSOCKET =================
    private void connectWebSocket() {

        try {
            client = new WebSocketClient(new URI("ws://localhost:8887")) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("✅ Connected");
                }

                @Override
                public void onMessage(String message) {

                    Platform.runLater(() -> {

                        String[] p = message.split("\\|", 4);

                        // ================= REACTION =================
                        if (p.length == 4 && p[0].equals("REACTION")) {

                            String user = p[2];
                            String type = p[3];

                            String emoji = switch (type) {
                                case "LIKE" -> "👍";
                                case "LOVE" -> "❤️";
                                case "HAHA" -> "😂";
                                case "WOW" -> "😮";
                                default -> "👍";
                            };

                            commentList.getItems().add("💥 " + user + " reacted " + emoji);
                        }

                        // ================= COMMENT =================
                        else if (p.length == 4 && p[0].equals("COMMENT")) {

                            String role = p[1];
                            String user = p[2];
                            String text = p[3];

                            commentList.getItems().add(user + " : " + text);
                        }

                        else {
                            commentList.getItems().add(message);
                        }

                        commentList.scrollTo(commentList.getItems().size() - 1);
                    });
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {}

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
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

        String username = "User";

        if (client != null && client.isOpen()) {
            client.send("COMMENT|USER|" + username + "|" + text);
        }

        commentField.clear();
    }

    // ================= REACTIONS =================
    @FXML private void sendLike()  { sendReaction("LIKE"); }
    @FXML private void sendLove()  { sendReaction("LOVE"); }
    @FXML private void sendHaha()  { sendReaction("HAHA"); }
    @FXML private void sendWow()   { sendReaction("WOW"); }

    private void sendReaction(String type) {

        String username = "User";

        if (client != null && client.isOpen()) {
            client.send("REACTION|USER|" + username + "|" + type);
        }
    }

    // ================= CHAT STYLE =================
    private void setupChatUI() {

        commentList.setStyle(
                "-fx-control-inner-background:#0f172a;" +
                        "-fx-background-color:#0f172a;" +
                        "-fx-border-color:transparent;"
        );

        commentList.setCellFactory(list -> new ListCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                setText(null);
                setGraphic(null);

                if (empty || item == null) return;

                Label label = new Label(item);
                label.setWrapText(true);
                label.setStyle(
                        "-fx-text-fill:white;" +
                                "-fx-padding:6 10;"
                );

                setGraphic(label);
            }
        });
    }
}