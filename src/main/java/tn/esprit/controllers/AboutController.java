package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.entities.Blog;
import tn.esprit.services.ServiceBlog;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    @FXML
    private TableView<Blog> blogTable;
    @FXML
    private TableColumn<Blog, Integer> idCol;
    @FXML
    private TableColumn<Blog, String> titleCol;
    @FXML
    private TableColumn<Blog, String> categoryCol;
    @FXML
    private TableColumn<Blog, String> dateCol;
    @FXML
    private TableColumn<Blog, Integer> commentsCol;
    @FXML
    private TableColumn<Blog, String> contentCol;
    @FXML
    private Label messageLabel;
    @FXML
    private Label subtitleLabel;

    private final ServiceBlog serviceBlog = new ServiceBlog();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        commentsCol.setCellValueFactory(new PropertyValueFactory<>("commentCount"));

        dateCol.setCellValueFactory(c -> {
            Blog b = c.getValue();
            if (b.getCreatedAt() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(b.getCreatedAt().toString());
        });

        contentCol.setCellValueFactory(c -> {
            String t = c.getValue().getContent();
            if (t == null) {
                return new SimpleStringProperty("");
            }
            String shortText = t.length() > 120 ? t.substring(0, 120) + "…" : t;
            return new SimpleStringProperty(shortText);
        });

        List<Blog> posts = serviceBlog.afficher();
        blogTable.setItems(FXCollections.observableArrayList(posts));
        subtitleLabel.setText(posts.size() + " article(s) — table `blog`");
        if (posts.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: #94a3b8;");
            messageLabel.setText("Aucun article. Vérifiez que la table blog existe et contient des données.");
        }
    }
}
