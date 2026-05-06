package tn.esprit.controllers.Stream;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import tn.esprit.entities.Video;
import tn.esprit.entities.VideoComment;
import tn.esprit.services.ServiceVideoComment;
import tn.esprit.services.ServiceVideoReaction;
import tn.esprit.services.VideoService;

import java.io.File;
import java.util.List;

public class AdminVideoController {

    @FXML
    private FlowPane videoContainer;

    @FXML
    private Label statusLabel;

    private final VideoService videoService = new VideoService();
    private final ServiceVideoComment commentService = new ServiceVideoComment();
    private final ServiceVideoReaction reactionService = new ServiceVideoReaction();

    @FXML
    public void initialize() {
        loadVideos();
    }

    private void loadVideos() {

        videoContainer.getChildren().clear();

        List<Video> videos = videoService.getAllVideos();

        for (Video v : videos) {

            VBox card = new VBox(10);
            card.setStyle("""
                -fx-background-color:#1e293b;
                -fx-padding:10;
                -fx-background-radius:12;
                -fx-pref-width:260;
            """);

            // 🖼 THUMBNAIL
            ImageView thumbnail = new ImageView();
            try {
                thumbnail.setImage(new Image(v.getThumbnail(), true));
            } catch (Exception e) {
                System.out.println("Thumbnail error");
            }

            thumbnail.setFitWidth(240);
            thumbnail.setFitHeight(130);

            // 🎯 TITLE
            Label title = new Label(v.getTitle());
            title.setStyle("-fx-text-fill:white; -fx-font-weight:bold;");

            // ❤️ REACTIONS
            int reactionsCount = reactionService.getTotalReactions(v.getId());
            Label reactions = new Label("❤️ " + reactionsCount + " reactions");
            reactions.setStyle("-fx-text-fill:#22c55e;");

            // 💬 COMMENTS
            VBox commentBox = new VBox(5);

            List<VideoComment> comments = commentService.findByVideo(v.getId());

            for (VideoComment c : comments) {

                HBox row = new HBox(10);

                Label txt = new Label(c.getUsername() + ": " + c.getBody());
                txt.setStyle("-fx-text-fill:white;");

                Button delete = new Button("❌");

                delete.setOnAction(e -> {
                    commentService.deleteComment(c.getId());
                    loadVideos();
                });

                row.getChildren().addAll(txt, delete);
                commentBox.getChildren().add(row);
            }

            ScrollPane scroll = new ScrollPane(commentBox);
            scroll.setPrefHeight(120);
            scroll.setFitToWidth(true);

            // 🗑 DELETE VIDEO
            Button deleteVideo = new Button("Delete Video");

            deleteVideo.setOnAction(e -> {
                videoService.deleteVideo(v.getId());
                loadVideos();
            });

            card.getChildren().addAll(
                    thumbnail,
                    title,
                    reactions,
                    scroll,
                    deleteVideo
            );

            videoContainer.getChildren().add(card);
        }
    }

    @FXML
    public void uploadFromPC() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Video");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("MP4 Files", "*.mp4")
        );

        File file = fileChooser.showOpenDialog(null);

        if (file == null) return;

        new Thread(() -> {

            Video v = videoService.uploadToCloudinary(file);

            if (v != null) {

                videoService.addVideo(v);

                Platform.runLater(this::loadVideos);
            }

        }).start();
    }
}