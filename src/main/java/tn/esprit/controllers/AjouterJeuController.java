package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Jeu;
import tn.esprit.services.ServiceJeu;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterJeuController implements Initializable {

    @FXML
    private TextField nomField;
    @FXML
    private TextField genreField;
    @FXML
    private TextField plateformeField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<String> statutCombo;
    @FXML
    private Label messageLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Button submitBtn;

    private final ServiceJeu serviceJeu = new ServiceJeu();
    private Jeu jeuToEdit = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statutCombo.setItems(FXCollections.observableArrayList(
                "Actif", "Inactif", "Bêta", "Maintenance"));
        statutCombo.getSelectionModel().selectFirst();
    }

    public void setJeuToEdit(Jeu j) {
        this.jeuToEdit = j;
        titleLabel.setText("Modifier le jeu");
        submitBtn.setText("Mettre à jour");
        nomField.setText(j.getNom());
        genreField.setText(j.getGenre() != null ? j.getGenre() : "");
        plateformeField.setText(j.getPlateforme() != null ? j.getPlateforme() : "");
        descriptionArea.setText(j.getDescription() != null ? j.getDescription() : "");
        if (j.getStatut() != null && !j.getStatut().isEmpty()) {
            statutCombo.setValue(j.getStatut());
        }
    }

    @FXML
    private void handleSubmit() {
        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Le nom est obligatoire.");
            return;
        }

        String statut = statutCombo.getEditor().getText();
        if (statut == null || statut.trim().isEmpty()) {
            statut = "Actif";
        }

        try {
            if (jeuToEdit == null) {
                Jeu jeu = new Jeu(
                        nomField.getText().trim(),
                        nullToEmpty(genreField.getText()),
                        nullToEmpty(plateformeField.getText()),
                        nullToEmpty(descriptionArea.getText()),
                        statut.trim());
                serviceJeu.ajouter(jeu);
                messageLabel.setStyle("-fx-text-fill: #4ade80;");
                messageLabel.setText("Jeu ajouté.");
            } else {
                jeuToEdit.setNom(nomField.getText().trim());
                jeuToEdit.setGenre(nullToEmpty(genreField.getText()));
                jeuToEdit.setPlateforme(nullToEmpty(plateformeField.getText()));
                jeuToEdit.setDescription(nullToEmpty(descriptionArea.getText()));
                jeuToEdit.setStatut(statut.trim());
                serviceJeu.modifier(jeuToEdit);
                messageLabel.setStyle("-fx-text-fill: #4ade80;");
                messageLabel.setText("Jeu modifié.");
            }
            scheduleClose();
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Erreur SQL : " + e.getMessage());
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private void scheduleClose() {
        new Thread(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {
            }
            javafx.application.Platform.runLater(() ->
                    ((Stage) nomField.getScene().getWindow()).close());
        }).start();
    }

    @FXML
    private void handleCancel() {
        ((Stage) nomField.getScene().getWindow()).close();
    }
}
