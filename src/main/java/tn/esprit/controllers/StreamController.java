package tn.esprit.controllers;

import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import tn.esprit.entities.Video;
import tn.esprit.entities.User;
import tn.esprit.services.VideoService;
import tn.esprit.services.ServiceStreamReaction;
import tn.esprit.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.io.File;

public class StreamController {

    @FXML
    private WebView webView;
    @FXML
    private GridPane videoContainer;
    @FXML
    private MediaView mediaView;

    // ====== REACTIONS (AJOUT) ======
    @FXML
    private HBox reactionBox;
    @FXML
    private Label totalLabel;
    private final ServiceStreamReaction reactionService = new ServiceStreamReaction();
    // TODO: si tu as plusieurs streams en DB, remplace 1 par l'id du stream actif
    private final int streamIdForReactions = 1;

    private File selectedFile;
    @FXML
    private TextField titleField;
    private final VideoService videoService = new VideoService();

    private void playVideoLocal(String path) {

        // 🔥 conversion Windows path → file URI
        String fullPath = "file:///C:/xampp2/htdocs/" + path;

        Media media = new Media(fullPath);
        MediaPlayer player = new MediaPlayer(media);

        mediaView.setMediaPlayer(player);
        player.play();
    }

    @FXML
    public void initialize() {
        loadStream();
        loadVideos();
        setupReactions();
        System.out.println("mediaView = " + mediaView);
        System.out.println("webView = " + webView);

    }

    private String currentUsername() {
        User u = SessionManager.getCurrentUser();
        if (u == null) {
            return "Invité";
        }
        try {
            if (u.getNom() != null && !u.getNom().isBlank()) {
                return u.getNom().trim();
            }
        } catch (Exception ignored) {
        }
        return (u.getEmail() != null && !u.getEmail().isBlank()) ? u.getEmail() : "Invité";
    }

    private void setupReactions() {
        if (reactionBox == null || totalLabel == null) {
            return;
        }

        reactionBox.getChildren().clear();
        for (String emoji : ServiceStreamReaction.REACTIONS) {
            Button b = new Button(emoji);
            b.setStyle("-fx-font-size:18px; -fx-background-color:#2d2d2d; -fx-text-fill:white;");
            b.setOnAction(e -> {
                boolean ok = reactionService.addEmojiReaction(emoji, currentUsername(), streamIdForReactions);
                if (!ok) {
                    totalLabel.setText("Réactions : échec enregistrement (voir console)");
                    return;
                }
                refreshReactionTotal();
            });
            reactionBox.getChildren().add(b);
        }

        refreshReactionTotal();
    }

    private void refreshReactionTotal() {
        if (totalLabel == null) {
            return;
        }
        int total = reactionService.getTotalReactions(streamIdForReactions);
        totalLabel.setText("Reactions: " + total);
    }

    private void loadStream() {

        String streamUrl = "http://192.168.100.81:8080/hls/match1.m3u8";


        WebEngine engine = webView.getEngine();

        String html =
                "<html><body style='margin:0;background:black;'>" +
                        "<video id='video' controls autoplay muted style='width:100%;height:100%;'></video>" +

                        "<script src='https://cdn.jsdelivr.net/npm/hls.js@latest'></script>" +

                        "<script>" +
                        "var video = document.getElementById('video');" +

                        "if(Hls.isSupported()){" +
                        "   var hls = new Hls();" +
                        "   hls.loadSource('" + streamUrl + "');" +
                        "   hls.attachMedia(video);" +
                        "} else if(video.canPlayType('application/vnd.apple.mpegurl')){" +
                        "   video.src = '" + streamUrl + "';" +
                        "}" +

                        "</script>" +
                        "</body></html>";

        engine.loadContent(html);
    }

    // =========================
    // 📌 UPLOAD + DB + LOCAL
    // =========================
    @FXML
    public void uploadVideo() {

        String folderPath = "C:\\Users\\Mouhamed Tayssir\\Videos";
        File folder = new File(folderPath);

        File[] files = folder.listFiles();

        if (files == null || files.length == 0) return;

        // =========================
        // 🔥 1. Trouver le dernier fichier mp4
        // =========================
        File lastFile = null;
        long lastModified = 0;

        for (File file : files) {

            if (file.getName().endsWith(".mp4")) {

                if (file.lastModified() > lastModified) {
                    lastModified = file.lastModified();
                    lastFile = file;
                }
            }
        }

        // =========================
        // 🔥 2. Ajouter seulement le dernier fichier
        // =========================
        if (lastFile != null) {

            String fileName = lastFile.getName();
            String dbPath = "videos/" + fileName;

            // ⚠️ éviter doublon
            if (!videoService.existsByPath(dbPath)) {

                Video v = new Video(fileName, dbPath);
                videoService.addVideo(v);

                System.out.println("✅ Ajouté: " + fileName);
            } else {
                System.out.println("⚠️ Vidéo déjà existante");
            }
        }

        loadVideos();

    }



    private void loadVideos() {

        videoContainer.getChildren().clear();
        int column = 0;
        int row = 0;

        User user = SessionManager.getCurrentUser();
        boolean isAdmin = false; // 🔥 MODE USER FORCÉ

        for (Video v : videoService.getLocalVideos("C:\\Users\\Mouhamed Tayssir\\Videos")) {

            VBox card = new VBox(8);
            card.getStyleClass().add("video-card");

// 🎬 ICON (thumbnail simple)
            Label thumbnail = new Label("🎬");
            thumbnail.setStyle("-fx-font-size:50px; -fx-text-fill:white;");

// 🎬 TITLE
            Label title = new Label(v.getTitle());
            title.getStyleClass().add("video-title");

// ▶ PLAY TEXT
            Label play = new Label("▶ Play");
            play.setStyle("-fx-text-fill:#00ffcc; -fx-font-size:12px;");

// CLICK EVENT
            card.setOnMouseClicked(e -> {
                System.out.println("VIDEO CLICKED: " + v.getPath());
                playVideo(v);
            });

// ADD ELEMENTS
            card.getChildren().addAll(thumbnail, title, play);





            if (isAdmin) {
                Button deleteBtn = new Button("🗑");
                deleteBtn.setStyle("-fx-background-color:red;-fx-text-fill:white;");

                deleteBtn.setOnAction(e -> {
                    videoService.deleteVideo(v.getId());
                    loadVideos();
                });

                card.getChildren().add(deleteBtn);
            }

            // ================= GRID POSITION =================
            videoContainer.add(card, column, row);

            column++;
            if (column == 3) {
                column = 0;
                row++;
            }
        }
    }


    private void playVideo(Video v) {

        String path = v.getPath();
        webView.setVisible(true);
        mediaView.setVisible(false);
        // =========================
        // 🎬 CAS 1 : HLS STREAM
        // =========================
        if (path.endsWith(".m3u8")) {

            WebEngine engine = webView.getEngine();

            String html =
                    "<html><body style='margin:0;background:black;'>" +
                            "<video id='video' controls autoplay style='width:100%;height:100%;'></video>" +
                            "<script src='https://cdn.jsdelivr.net/npm/hls.js@latest'></script>" +
                            "<script>" +
                            "var video = document.getElementById('video');" +
                            "if(Hls.isSupported()){" +
                            "   var hls = new Hls();" +
                            "   hls.loadSource('" + path + "');" +
                            "   hls.attachMedia(video);" +
                            "} else {" +
                            "   video.src = '" + path + "';" +
                            "}" +
                            "</script>" +
                            "</body></html>";

            webView.setVisible(true);
            mediaView.setVisible(false);
            engine.loadContent(html);
        }

        // =========================
        // 🎬 CAS 2 : MP4 LOCAL
        // =========================
        else {
            webView.setVisible(false);
            mediaView.setVisible(true);
            try {
                Media media = new Media(new File(path).toURI().toString());
                MediaPlayer player = new MediaPlayer(media);

                mediaView.setMediaPlayer(player);

                webView.setVisible(false);
                mediaView.setVisible(true);

                player.play();

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Erreur lecture vidéo: " + path);
            }
        }
    }
}