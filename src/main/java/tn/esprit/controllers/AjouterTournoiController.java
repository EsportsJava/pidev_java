package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.esprit.entities.Jeu;
import tn.esprit.entities.Tournoi;
import tn.esprit.services.ServiceJeu;
import tn.esprit.services.ServiceTournoi;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class AjouterTournoiController implements Initializable {

    @FXML
    private TextField nomField;
    @FXML
    private ComboBox<Jeu> jeuCombo;
    @FXML
    private DatePicker dateDebutPicker;
    @FXML
    private DatePicker dateFinPicker;
    @FXML
    private ComboBox<String> statutCombo;
    @FXML
    private TextField typeField;
    @FXML
    private TextField maxParticipantsField;
    @FXML
    private TextField cagnotteField;
    @FXML
    private DatePicker dateLimitePicker;
    @FXML
    private TextField fraisField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label messageLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Button submitBtn;

    private final ServiceTournoi serviceTournoi = new ServiceTournoi();
    private final ServiceJeu serviceJeu = new ServiceJeu();
    private Tournoi tournoiToEdit = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statutCombo.setItems(FXCollections.observableArrayList(
                "Planifié", "En cours", "Terminé", "Annulé"));
        statutCombo.getSelectionModel().selectFirst();

        jeuCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Jeu jeu) {
                return jeu == null ? "" : jeu.getNom();
            }

            @Override
            public Jeu fromString(String s) {
                return null;
            }
        });

        loadJeuxCombo();
    }

    private void loadJeuxCombo() {
        try {
            List<Jeu> jeux = serviceJeu.getAll();
            jeuCombo.setItems(FXCollections.observableArrayList(jeux));
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Erreur chargement jeux : " + e.getMessage());
        }
    }

    public void setTournoiToEdit(Tournoi t) {
        this.tournoiToEdit = t;
        titleLabel.setText("Modifier le tournoi");
        submitBtn.setText("Mettre à jour");

        nomField.setText(t.getNom());
        typeField.setText(t.getType() != null ? t.getType() : "");
        maxParticipantsField.setText(String.valueOf(t.getMaxParticipants()));
        cagnotteField.setText(String.valueOf(t.getCagnotte()));
        fraisField.setText(String.valueOf(t.getFraisInscription()));
        descriptionArea.setText(t.getDescription() != null ? t.getDescription() : "");

        dateDebutPicker.setValue(toLocalDate(t.getDateDebut()));
        dateFinPicker.setValue(toLocalDate(t.getDateFin()));
        dateLimitePicker.setValue(toLocalDate(t.getDateInscriptionLimite()));

        if (t.getStatut() != null && !t.getStatut().isEmpty()) {
            statutCombo.setValue(t.getStatut());
        }

        for (Jeu j : jeuCombo.getItems()) {
            if (j.getId() == t.getJeuId()) {
                jeuCombo.getSelectionModel().select(j);
                break;
            }
        }
    }

    private static LocalDate toLocalDate(Date d) {
        if (d == null) {
            return LocalDate.now();
        }
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date toDate(LocalDate ld) {
        if (ld == null) {
            return null;
        }
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void showValidationError(String message) {
        messageLabel.setStyle("-fx-text-fill: #f87171;");
        messageLabel.setText(message);
    }

    @FXML
    private void handleSubmit() {
        if (isBlank(nomField.getText())) {
            showValidationError("Le nom est obligatoire.");
            return;
        }

        Jeu selected = jeuCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showValidationError("Choisissez un jeu.");
            return;
        }

        LocalDate ldDebut = dateDebutPicker.getValue();
        LocalDate ldFin = dateFinPicker.getValue();
        LocalDate ldLimite = dateLimitePicker.getValue();
        if (ldDebut == null || ldFin == null || ldLimite == null) {
            showValidationError("Renseignez les trois dates.");
            return;
        }
        if (ldFin.isBefore(ldDebut)) {
            showValidationError("La date de fin doit être après ou égale à la date de début.");
            return;
        }
        if (ldLimite.isAfter(ldDebut)) {
            showValidationError("La limite d'inscription doit être avant ou égale à la date de début.");
            return;
        }
        if (ldLimite.isAfter(ldFin)) {
            showValidationError("La limite d'inscription ne peut pas dépasser la date de fin.");
            return;
        }

        String maxStr = maxParticipantsField.getText() != null ? maxParticipantsField.getText().trim() : "";
        if (maxStr.isEmpty()) {
            showValidationError("Indiquez le nombre maximum de participants.");
            return;
        }
        int maxParticipants;
        try {
            maxParticipants = Integer.parseInt(maxStr);
        } catch (NumberFormatException e) {
            showValidationError("Max participants : nombre entier invalide.");
            return;
        }
        if (maxParticipants <= 0) {
            showValidationError("Max participants doit être supérieur à 0.");
            return;
        }

        String cagStr = cagnotteField.getText() != null ? cagnotteField.getText().trim().replace(',', '.') : "";
        String fraisStr = fraisField.getText() != null ? fraisField.getText().trim().replace(',', '.') : "";
        if (cagStr.isEmpty() || fraisStr.isEmpty()) {
            showValidationError("Renseignez la cagnotte et les frais (0 autorisé).");
            return;
        }
        double cagnotte;
        double frais;
        try {
            cagnotte = Double.parseDouble(cagStr);
            frais = Double.parseDouble(fraisStr);
        } catch (NumberFormatException e) {
            showValidationError("Cagnotte / frais : nombre invalide.");
            return;
        }
        if (cagnotte < 0 || frais < 0) {
            showValidationError("Cagnotte et frais doivent être positifs ou nuls.");
            return;
        }

        String statut = statutCombo.getEditor().getText();
        if (statut == null || statut.trim().isEmpty()) {
            statut = statutCombo.getValue();
        }
        if (statut == null || statut.trim().isEmpty()) {
            showValidationError("Le statut est obligatoire.");
            return;
        }

        if (isBlank(typeField.getText())) {
            showValidationError("Le type de tournoi est obligatoire.");
            return;
        }
        if (isBlank(descriptionArea.getText())) {
            showValidationError("La description est obligatoire.");
            return;
        }

        String type = typeField.getText().trim();
        String description = descriptionArea.getText().trim();

        try {
            if (tournoiToEdit == null) {
                Tournoi tournoi = new Tournoi(
                        nomField.getText().trim(),
                        toDate(ldDebut),
                        toDate(ldFin),
                        statut.trim(),
                        type,
                        maxParticipants,
                        cagnotte,
                        toDate(ldLimite),
                        frais,
                        description,
                        selected.getId());
                serviceTournoi.ajouter(tournoi);
                messageLabel.setStyle("-fx-text-fill: #4ade80;");
                messageLabel.setText("Tournoi ajouté.");
            } else {
                tournoiToEdit.setNom(nomField.getText().trim());
                tournoiToEdit.setDateDebut(toDate(ldDebut));
                tournoiToEdit.setDateFin(toDate(ldFin));
                tournoiToEdit.setStatut(statut.trim());
                tournoiToEdit.setType(type);
                tournoiToEdit.setMaxParticipants(maxParticipants);
                tournoiToEdit.setCagnotte(cagnotte);
                tournoiToEdit.setDateInscriptionLimite(toDate(ldLimite));
                tournoiToEdit.setFraisInscription(frais);
                tournoiToEdit.setDescription(description);
                tournoiToEdit.setJeuId(selected.getId());
                serviceTournoi.modifier(tournoiToEdit);
                messageLabel.setStyle("-fx-text-fill: #4ade80;");
                messageLabel.setText("Tournoi modifié.");
            }
            scheduleClose();
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Erreur SQL : " + e.getMessage());
        }
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
