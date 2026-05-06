package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.stage.Stage;
import tn.esprit.utils.SessionManager;

import java.io.IOException;

public class NavbarController {

    @FXML
    private Button profileUserButton;

    @FXML
    public void initialize() {
        updateProfileButtonVisibility();
    }

    private void updateProfileButtonVisibility() {
        if (profileUserButton != null) {
            boolean isUserLoggedIn = SessionManager.getCurrentUser() != null;
            profileUserButton.setVisible(isUserLoggedIn);
            profileUserButton.setManaged(isUserLoggedIn);
        }
    }

    @FXML
    private void goToHome(ActionEvent event) {
        try {
            loadScene(event, "/home.fxml", "Home");
        } catch (IOException e) {
            System.out.println("Unable to open home.");
        }
    }

    @FXML
    private void goToTournement(ActionEvent event) {
        try {
            loadScene(event, "/tournoiCatalog.fxml", "Tournoi");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to open tournement: " + e.getMessage());
        }
    }

    @FXML
    private void goToJeux(ActionEvent event) {
        try {
            loadScene(event, "/jeuxCatalog.fxml", "Jeux");
        } catch (IOException e) {
            System.out.println("Unable to open jeux.");
        }
    }

    @FXML
    private void goToTeam(ActionEvent event) {
        try {
            loadScene(event, "/equipe/afficherEquipe.fxml", "Team");
        } catch (IOException e) {
            System.out.println("Unable to open team.");
        }
    }

    @FXML
    private void goToAbout(ActionEvent event) {
        try {
            loadScene(event, "/AfficherBlogs.fxml", "Blogs");
        } catch (IOException e) {
            System.out.println("Unable to open blogs.");
        }
    }

    @FXML
    private void goToStats(ActionEvent event) {
        try {
            loadScene(event, "/stats.fxml", "Stats");
        } catch (IOException e) {
            System.out.println("Unable to open stats.");
        }
    }

    @FXML
    private void goToMain(ActionEvent event) {
        try {
            loadScene(event, "/afficherProduits.fxml", "Product");
        } catch (IOException e) {
            System.out.println("Unable to open products.");
        }
    }

    @FXML
    private void goToUser(ActionEvent event) {
        try {
            loadScene(event, "/ProfileUser.fxml", "Profile User");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to open profile.");
        }
    }

    private void loadScene(ActionEvent event, String fxml, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }

    @FXML
    private void goToCart(ActionEvent event) {
        try {
            loadScene(event, "/cart.fxml", "Mon Panier");
        } catch (IOException e) {
            e.printStackTrace(); // ← changez ça
            System.out.println("Erreur: " + e.getMessage());
        }
    }
    // ================= USER VIDEO =================

    @FXML
    private void showUserVideos(ActionEvent event) {
        try {
            loadScene(event, "/user-video-view.fxml", "Videos User");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void goToLive(ActionEvent event) {
        try {
            loadScene(event, "/UserStream.fxml", "Live Stream");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to open live stream.");
        }
    }

    @FXML
    private void goToNotifications(ActionEvent event) {
        try {
            loadScene(event, "/notifications.fxml", "Notifications");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to open notifications: " + e.getMessage());
        }
    }
}