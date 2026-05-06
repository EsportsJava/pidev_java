package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Blog;
import tn.esprit.services.ServiceBlog;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class BlogsController implements Initializable {

    @FXML private TableView<Blog>              blogsTable;
    @FXML private TableColumn<Blog, Integer>   idCol;
    @FXML private TableColumn<Blog, String>    titleCol;
    @FXML private TableColumn<Blog, String>    categoryCol;
    @FXML private TableColumn<Blog, String>    contentCol;
    @FXML private TableColumn<Blog, Timestamp> dateCol;
    @FXML private TableColumn<Blog, Void>      actionsCol;
    @FXML private TextField                    searchField;
    @FXML private Label                        messageLabel;
    @FXML private Label                        totalBlogsLabel;
    @FXML private Label                        totalCommentsLabel;

    private final ServiceBlog serviceBlog = new ServiceBlog();
    private ObservableList<Blog> allBlogs = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        addActionsColumn();
        loadBlogs();
    }

    private void loadBlogs() {
        List<Blog> list = serviceBlog.afficher();
        allBlogs = FXCollections.observableArrayList(list);
        blogsTable.setItems(allBlogs);
        totalBlogsLabel.setText(String.valueOf(list.size()));
        int totalComments = list.stream().mapToInt(Blog::getCommentCount).sum();
        totalCommentsLabel.setText(String.valueOf(totalComments));
    }

    private void addActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox   box       = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.setStyle(
                        "-fx-background-color: #f59e0b; -fx-text-fill: white;" +
                                "-fx-cursor: hand; -fx-background-radius: 6;");
                deleteBtn.setStyle(
                        "-fx-background-color: #ef4444; -fx-text-fill: white;" +
                                "-fx-cursor: hand; -fx-background-radius: 6;");

                editBtn.setOnAction(e -> {
                    Blog blog = getTableView().getItems().get(getIndex());
                    openModifier(blog);
                });

                deleteBtn.setOnAction(e -> {
                    Blog blog = getTableView().getItems().get(getIndex());
                    confirmerSuppression(blog);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML
    private void handleNewBlog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/AjouterBlog.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un Blog");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadBlogs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openModifier(Blog blog) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/AjouterBlog.fxml"));
            Parent root = loader.load();

            AjouterBlogController ctrl = loader.getController();
            ctrl.setBlogToEdit(blog);

            Stage stage = new Stage();
            stage.setTitle("Modifier un Blog");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadBlogs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void confirmerSuppression(Blog blog) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer : " + blog.getTitle() + " ?");
        alert.setContentText("Cette action est irréversible.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                serviceBlog.supprimer(blog.getId());
                loadBlogs();
                messageLabel.setText("✅ Blog supprimé.");
                messageLabel.setStyle("-fx-text-fill: #4ade80;");
            }
        });
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.isEmpty()) {
            blogsTable.setItems(allBlogs);
        } else {
            ObservableList<Blog> filtered = allBlogs.stream()
                    .filter(b -> b.getTitle().toLowerCase().contains(query)
                            || b.getCategory().toLowerCase().contains(query))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            blogsTable.setItems(filtered);
        }
    }
    
}