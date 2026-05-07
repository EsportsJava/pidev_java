package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Tournoi;
import tn.esprit.entities.User;
import tn.esprit.services.NotificationService;
import tn.esprit.services.ServiceEquipe;
import tn.esprit.services.ServiceTournoiInscription;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class TournoiInscriptionController implements Initializable {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @FXML
    private Label titleLabel;
    @FXML
    private Label tournoiLabel;
    @FXML
    private Label jeuLabel;
    @FXML
    private Label placesLabel;
    @FXML
    private ComboBox<Equipe> equipeComboBox;
    @FXML
    private Label noEquipeLabel;
    @FXML
    private TextField nomField;
    @FXML
    private TextField emailField;
    @FXML
    private Label statusLabel;

    private final ServiceTournoiInscription service = new ServiceTournoiInscription();
    private final ServiceEquipe serviceEquipe = new ServiceEquipe();
    private final NotificationService notificationService = new NotificationService();
    private Tournoi tournoi;
    private String jeuNom;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        statusLabel.setText("Prêt.");
        statusLabel.setStyle("-fx-text-fill: #cbd5e1;");

        // Configure ComboBox to show team names
        equipeComboBox.setButtonCell(new EquipeListCell());
        equipeComboBox.setCellFactory(param -> new EquipeListCell());

        User u = SessionManager.getCurrentUser();
        if (u != null) {
            if (u.getNom() != null && !u.getNom().isBlank()) {
                nomField.setText(u.getNom());
            }
            if (u.getEmail() != null && !u.getEmail().isBlank()) {
                emailField.setText(u.getEmail());
            }
            loadOwnedEquipes(u.getId());
        } else {
            noEquipeLabel.setText("Connectez-vous pour voir vos équipes.");
        }
    }

    /**
     * Load teams owned by the current user into the ComboBox.
     */
    private void loadOwnedEquipes(int userId) {
        try {
            List<Equipe> owned = service.getOwnedEquipesForUser(userId);
            equipeComboBox.getItems().clear();
            if (owned.isEmpty()) {
                noEquipeLabel.setText("⚠ Vous ne possédez aucune équipe. Créez une équipe d'abord.");
                equipeComboBox.setDisable(true);
            } else {
                noEquipeLabel.setText("");
                equipeComboBox.getItems().addAll(owned);
                equipeComboBox.getSelectionModel().selectFirst();
                equipeComboBox.setDisable(false);
            }
        } catch (SQLException e) {
            noEquipeLabel.setText("Erreur chargement équipes: " + e.getMessage());
        }
    }

    public void setTournoi(Tournoi t, String jeuNom) {
        this.tournoi = t;
        this.jeuNom = jeuNom;
        refreshHeader();
    }

    private void refreshHeader() {
        if (tournoi == null) {
            return;
        }
        titleLabel.setText("Inscription tournoi");
        tournoiLabel.setText(tournoi.getNom() == null ? "Tournoi" : tournoi.getNom());
        jeuLabel.setText(jeuNom == null ? "—" : jeuNom);
        try {
            int used = service.countByTournoi(tournoi.getId());
            int max = Math.max(0, tournoi.getMaxParticipants());
            placesLabel.setText(used + " / " + max);
        } catch (SQLException e) {
            placesLabel.setText("—");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/tournoiCatalog.fxml"));
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1280, 720));
        stage.setTitle("Tournoi");
        stage.show();
    }

    @FXML
    private void handleSubmit() {
        if (tournoi == null) {
            showError("Tournoi introuvable.");
            return;
        }

        User u = SessionManager.getCurrentUser();
        if (u == null || u.getId() <= 0) {
            showError("Connectez-vous avant de vous inscrire.");
            return;
        }

        // Validate team selection
        Equipe selectedEquipe = equipeComboBox.getSelectionModel().getSelectedItem();
        if (selectedEquipe == null) {
            showError("Veuillez sélectionner une équipe à inscrire.");
            return;
        }

        String nom = safe(nomField.getText());
        String email = safe(emailField.getText());

        if (nom.isEmpty()) {
            showError("Le nom est obligatoire.");
            return;
        }
        if (email.isEmpty() || !EMAIL.matcher(email).matches()) {
            showError("Email invalide.");
            return;
        }

        try {
            service.ensureTable();
            int used = service.countByTournoi(tournoi.getId());
            int max = Math.max(0, tournoi.getMaxParticipants());
            if (max > 0 && used >= max) {
                showError("Tournoi complet. (" + used + "/" + max + ")");
                return;
            }
            // Check if this team is already registered
            if (service.existsForEquipe(tournoi.getId(), selectedEquipe.getId())) {
                showError("L'équipe \"" + selectedEquipe.getNom() + "\" est déjà inscrite à ce tournoi.");
                return;
            }
            // Register with equipe_id instead of user_id
            service.inscrire(tournoi.getId(), u.getId(), selectedEquipe.getId(), nom, email);
            notificationService.createNotificationIfAbsent(
                    u.getId(),
                    "inscription_confirmed",
                    "Inscription confirmée",
                    "L'équipe \"" + selectedEquipe.getNom() + "\" est inscrite au tournoi \"" + (tournoi.getNom() == null ? "Tournoi" : tournoi.getNom()) + "\".",
                    "inscription:" + tournoi.getId() + ":equipe:" + selectedEquipe.getId());
            statusLabel.setStyle("-fx-text-fill: #22c55e;");
            statusLabel.setText("✅ Inscription confirmée pour l'équipe \"" + selectedEquipe.getNom() + "\".");
            refreshHeader();
        } catch (SQLException e) {
            showError("Erreur inscription: " + e.getMessage());
        }
    }

    @FXML
    private void handleGoTeams(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/equipe/afficherEquipe.fxml"));
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1280, 720));
        stage.setTitle("Team");
        stage.show();
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #ef4444;");
        statusLabel.setText(msg);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Custom ListCell to display Equipe name in the ComboBox.
     */
    private static class EquipeListCell extends ListCell<Equipe> {
        @Override
        protected void updateItem(Equipe item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getNom() + "  (ID: " + item.getId() + ")");
            }
        }
    }
}
