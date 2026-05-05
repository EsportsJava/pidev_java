package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Blog;
import tn.esprit.services.ServiceBlog;
import tn.esprit.services.ServiceBlogLike;
import tn.esprit.services.ServiceBlogRating;
import tn.esprit.services.ServiceComment;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AfficherBlogsController implements Initializable {

    @FXML private FlowPane blogsContainer;
    @FXML private TextField searchField;

    private final ServiceBlogLike serviceLike = new ServiceBlogLike();
    private final ServiceBlogRating serviceRating = new ServiceBlogRating();
    private final ServiceComment serviceComment = new ServiceComment();
    private final ServiceBlog serviceBlog = new ServiceBlog();

    private static final String HTDOCS_PATH = "C:/xampp/htdocs/blog_images/";

    private List<Blog> allBlogs;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        allBlogs = serviceBlog.afficher();
        displayBlogs(allBlogs);
    }

    private void displayBlogs(List<Blog> blogs) {
        blogsContainer.getChildren().clear();

        if (blogs.isEmpty()) {
            Label empty = new Label("Aucun blog disponible.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");
            blogsContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < blogs.size(); i++) {
            blogsContainer.getChildren().add(
                    i == 0 ? createFeaturedCard(blogs.get(i)) : createArticleCard(blogs.get(i))
            );
        }
    }

    private HBox createFeaturedCard(Blog blog) {
        HBox card = new HBox();
        card.setPrefWidth(860);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-width: 0.5;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: null;"
        );

        StackPane imgPane = buildImagePane(blog, 340, 240);

        VBox body = new VBox(10);
        body.setPadding(new Insets(28));
        body.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(body, Priority.ALWAYS);

        Label featuredBadge = buildBadge("A la une", "#FEF3C7", "#92400E");
        Label tag = buildBadge(
                blog.getCategory() != null && blog.getCategory().length() > 20
                        ? "Blog" : (blog.getCategory() != null ? blog.getCategory() : "Blog"),
                "#EDE9FE", "#5B21B6");

        Label title = new Label(blog.getTitle());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        title.setWrapText(true);

        String descText = blog.getCategory() != null ? blog.getCategory() : "";
        if (descText.length() > 140) {
            descText = descText.substring(0, 140) + "...";
        }
        Label desc = new Label(descText);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-line-spacing: 3;");
        desc.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox footer = buildFooter(blog);

        body.getChildren().addAll(featuredBadge, tag, title, desc, spacer, footer);
        card.getChildren().addAll(imgPane, body);

        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> openBlogDetail(blog));

        return card;
    }

    private VBox createArticleCard(Blog blog) {
        VBox card = new VBox();
        card.setPrefWidth(260);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-width: 0.5;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;"
        );

        StackPane imgPane = buildImagePane(blog, 260, 160);

        VBox body = new VBox(8);
        body.setPadding(new Insets(14));
        VBox.setVgrow(body, Priority.ALWAYS);

        Label tag = buildBadge("Blog", "#EDE9FE", "#5B21B6");

        Label title = new Label(blog.getTitle());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        title.setWrapText(true);

        String descText = blog.getCategory() != null ? blog.getCategory() : "";
        if (descText.length() > 80) {
            descText = descText.substring(0, 80) + "...";
        }
        Label desc = new Label(descText);
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-line-spacing: 2;");
        desc.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        body.getChildren().addAll(tag, title, desc, spacer);

        HBox footer = buildFooter(blog);
        footer.setStyle(
                "-fx-border-color: #e2e8f0;" +
                        "-fx-border-width: 0.5 0 0 0;" +
                        "-fx-padding: 10 14 10 14;"
        );

        card.getChildren().addAll(imgPane, body, footer);

        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> openBlogDetail(blog));

        return card;
    }

    private StackPane buildImagePane(Blog blog, double w, double h) {
        StackPane pane = new StackPane();
        pane.setPrefSize(w, h);
        pane.setMinSize(w, h);
        pane.setMaxSize(w, h);
        pane.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 12 0 0 12;");

        File imgFile = new File(HTDOCS_PATH + blog.getContent());
        if (imgFile.exists()) {
            ImageView iv = new ImageView(new Image(imgFile.toURI().toString()));
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(false);
            pane.getChildren().add(iv);
        } else {
            Label icon = new Label("🖼");
            icon.setStyle("-fx-font-size: 32px; -fx-opacity: 0.25;");
            pane.getChildren().add(icon);
        }
        return pane;
    }

    private Label buildBadge(String text, String bg, String color) {
        Label badge = new Label(text);
        badge.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3 10 3 10;" +
                        "-fx-background-radius: 20;"
        );
        return badge;
    }

    private HBox buildFooter(Blog blog) {
        String initials = blog.getTitle() != null && blog.getTitle().length() >= 2
                ? blog.getTitle().substring(0, 2).toUpperCase() : "BL";
        Label avatar = new Label(initials);
        avatar.setPrefSize(26, 26);
        avatar.setMinSize(26, 26);
        avatar.setMaxSize(26, 26);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color: #EDE9FE; -fx-text-fill: #5B21B6;" +
                        "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 13;");

        String dateStr = blog.getCreatedAt() != null
                ? blog.getCreatedAt().toString().substring(0, 10) : "";
        Label date = new Label(dateStr);
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        int commentCount = serviceComment.getCountByBlogId(blog.getId());
        Label comments = new Label("💬 " + commentCount);
        comments.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        int likeCount = serviceLike.getLikeCount(blog.getId());
        Label likes = new Label("❤️ " + likeCount);
        likes.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        double avg = serviceRating.getAverageRating(blog.getId());
        int ratingCount = serviceRating.getRatingCount(blog.getId());
        Label rating = new Label(ratingCount > 0 ? String.format("⭐ %.1f", avg) : "⭐ -");
        rating.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        HBox footer = new HBox(8, avatar, date, spacer, likes, rating, comments);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 14, 10, 14));
        return footer;
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            displayBlogs(allBlogs);
        } else {
            List<Blog> filtered = allBlogs.stream()
                    .filter(b -> b.getTitle().toLowerCase().contains(query)
                            || (b.getCategory() != null && b.getCategory().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
            displayBlogs(filtered);
        }
    }

    private void openBlogDetail(Blog blog) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BlogDetail.fxml"));
            Parent root = loader.load();

            BlogDetailController controller = loader.getController();
            controller.setBlog(blog);

            Stage stage = (Stage) blogsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
