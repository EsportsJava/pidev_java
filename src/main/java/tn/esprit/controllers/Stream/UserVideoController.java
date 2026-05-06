package tn.esprit.controllers.Stream;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import tn.esprit.entities.Video;
import tn.esprit.entities.VideoComment;
import tn.esprit.services.ServiceVideoComment;
import tn.esprit.services.ServiceVideoReaction;
import tn.esprit.services.VideoService;

import java.util.List;

public class UserVideoController {

    @FXML private FlowPane videoContainer;
    @FXML private MediaView mediaView;
    @FXML private TextField commentField;
    @FXML private ListView<String> commentList;
    @FXML private Label currentVideoLabel;
    @FXML private Label reactionLabel;

    private final VideoService videoService = new VideoService();
    private final ServiceVideoComment commentService = new ServiceVideoComment();
    private final ServiceVideoReaction reactionService = new ServiceVideoReaction();

    private Video currentVideo;

    @FXML
    public void initialize() {
        loadVideos();
    }

    // ================= VIDEOS WITH THUMBNAILS =================
    private void loadVideos() {

        videoContainer.getChildren().clear();

        List<Video> videos = videoService.getAllVideos();

        for (Video v : videos) {

            VBox card = new VBox(5);
            card.setStyle("""
                -fx-background-color:#1e293b;
                -fx-padding:8;
                -fx-background-radius:10;
                -fx-cursor:hand;
            """);

            // 🎬 THUMBNAIL
            ImageView img = new ImageView();

            try {
                if (v.getThumbnail() != null) {
                    img.setImage(new Image(v.getThumbnail(), true));
                }
            } catch (Exception e) {
                System.out.println("Thumbnail error");
            }

            img.setFitWidth(200);
            img.setFitHeight(120);
            img.setPreserveRatio(false);

            // 🎯 TITLE
            Label title = new Label(v.getTitle());
            title.setStyle("-fx-text-fill:white;");

            card.getChildren().addAll(img, title);

            // CLICK VIDEO
            card.setOnMouseClicked(e -> playVideo(v));

            videoContainer.getChildren().add(card);
        }
    }

    // ================= PLAY VIDEO =================
    private void playVideo(Video v) {

        currentVideo = v;

        try {
            Media media = new Media(v.getPath());
            MediaPlayer player = new MediaPlayer(media);

            mediaView.setMediaPlayer(player);
            player.play();

        } catch (Exception e) {
            e.printStackTrace();
        }

        currentVideoLabel.setText(v.getTitle());

        loadComments();
        loadReactions();
    }

    // ================= ADD COMMENT =================
    @FXML
    private void addComment() {

        if (currentVideo == null) {
            showAlert("Select a video first !");
            return;
        }

        String text = commentField.getText();

        if (text == null || text.trim().isEmpty()) {
            showAlert("Write something !");
            return;
        }

        boolean ok = commentService.add(
                currentVideo.getId(),
                "User",
                text.trim()
        );

        if (ok) {
            commentField.clear();
            loadComments();
        } else {
            showAlert("Error saving comment");
        }
    }

    // ================= LOAD COMMENTS =================
    private void loadComments() {

        commentList.getItems().clear();

        if (currentVideo == null) return;

        List<VideoComment> comments =
                commentService.findByVideo(currentVideo.getId());

        for (VideoComment c : comments) {
            commentList.getItems().add(
                    c.getUsername() + " : " + c.getBody()
            );
        }
    }

    // ================= REACTIONS =================
    @FXML private void like() { addReaction("❤️"); }
    @FXML private void haha() { addReaction("😂"); }
    @FXML private void wow() { addReaction("😮"); }
    @FXML private void angry() { addReaction("😡"); }

    private void addReaction(String type) {

        if (currentVideo == null) {
            showAlert("Select a video first !");
            return;
        }

        reactionService.addReaction(type, "User", currentVideo.getId());

        loadReactions();
    }

    private void loadReactions() {

        if (currentVideo == null) return;

        int total = reactionService.getTotalReactions(currentVideo.getId());

        reactionLabel.setText(total + " reactions");
    }

    // ================= ALERT =================
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.show();
    }
}