package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private StackPane contentArea;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadPage("/produits.fxml");
    }

    @FXML
    public void showUsers() {
        loadPage("/users.fxml");
    }

    @FXML
    public void showProducts() {
        loadPage("/produits.fxml");
    }

    @FXML
    public void showCategories() {
        loadPage("/categories.fxml");
    }

    @FXML
    public void showJeux() {
        loadPage("/jeuxDashboard.fxml");
    }

    @FXML
    public void showTournois() {
        loadPage("/tournoiDashboard.fxml");
    }

    @FXML
    public void showEquipes() {
        loadPage("/equipe/equipeDashboard.fxml");
    }

    @FXML
    public void showMatchGames() {
        loadPage("/matchgame/matchGameDashboard.fxml");
    }

    @FXML
    public void showBlogs() {
        loadPage("/Blogs.fxml");
    }

    @FXML
    public void showStatistiques() {
        loadPage("/statistiques.fxml");
    }

    @FXML
    public void showHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/home.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("Esports Community");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showStream() {
        loadPage("/stream.fxml");
    }

    @FXML
    public void showOrders() {
        loadPage("/orders.fxml");
    }

    private void loadPage(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent view = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
