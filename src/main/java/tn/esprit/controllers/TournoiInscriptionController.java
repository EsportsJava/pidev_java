package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.entities.Tournoi;
import tn.esprit.entities.User;
import tn.esprit.services.NotificationService;
import tn.esprit.services.ServiceEquipe;
import tn.esprit.services.ServiceTournoiInscription;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
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
        User u = SessionManager.getCurrentUser();
        if (u != null) {
            if (u.getNom() != null && !u.getNom().isBlank()) {
                nomField.setText(u.getNom());
            }
            if (u.getEmail() != null && !u.getEmail().isBlank()) {
                emailField.setText(u.getEmail());
            }
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
            showError("Connectez-vous puis rejoignez une équipe avant de vous inscrire.");
            return;
        }
        try {
            if (!serviceEquipe.isUserInAnyEquipe(u.getId())) {
                showError("Vous devez rejoindre une équipe avant de vous inscrire à un tournoi.");
                return;
            }
        } catch (SQLException e) {
            showError("Vérification équipe impossible: " + e.getMessage());
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

        Integer userId = u.getId();

        try {
            service.ensureTable();
            int used = service.countByTournoi(tournoi.getId());
            int max = Math.max(0, tournoi.getMaxParticipants());
            if (max > 0 && used >= max) {
                showError("Tournoi complet. (" + used + "/" + max + ")");
                return;
            }
            if (service.existsForUser(tournoi.getId(), userId, email)) {
                showError("Vous êtes déjà inscrit à ce tournoi.");
                return;
            }
            service.inscrire(tournoi.getId(), userId, nom, email);
            notificationService.createNotificationIfAbsent(
                    userId,
                    "inscription_confirmed",
                    "Inscription confirmée",
                    "Votre inscription au tournoi \"" + (tournoi.getNom() == null ? "Tournoi" : tournoi.getNom()) + "\" est confirmée.",
                    "inscription:" + tournoi.getId() + ":" + userId);
            statusLabel.setStyle("-fx-text-fill: #22c55e;");
            statusLabel.setText("Inscription confirmée.");
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
}

