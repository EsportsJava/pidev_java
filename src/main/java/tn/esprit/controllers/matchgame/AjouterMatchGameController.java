package tn.esprit.controllers.matchgame;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.MatchGame;
import tn.esprit.entities.Tournoi;
import tn.esprit.services.ServiceEquipe;
import tn.esprit.services.ServiceMatchGame;
import tn.esprit.services.ServiceTournoi;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AjouterMatchGameController implements Initializable {

    @FXML
    private Label titleLabel;
    @FXML
    private DatePicker dateMatchPicker;
    @FXML
    private TextField score1Field;
    @FXML
    private TextField score2Field;
    @FXML
    private ComboBox<String> statutCombo;
    @FXML
    private ComboBox<OptionItem> equipe1Combo;
    @FXML
    private ComboBox<OptionItem> equipe2Combo;
    @FXML
    private ComboBox<OptionItem> tournoiCombo;
    @FXML
    private Label messageLabel;
    @FXML
    private Button submitBtn;

    private final ServiceMatchGame serviceMatchGame = new ServiceMatchGame();
    private final ServiceEquipe serviceEquipe = new ServiceEquipe();
    private final ServiceTournoi serviceTournoi = new ServiceTournoi();
    private MatchGame matchToEdit;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateMatchPicker.setValue(LocalDate.now());
        statutCombo.setItems(FXCollections.observableArrayList(
            "Scheduled", "Finished", "Planifie", "En cours", "Termine", "Annule"
        ));
        statutCombo.getSelectionModel().selectFirst();

        try {
            loadCombos();
        } catch (SQLException e) {
            showError("Erreur chargement listes : " + e.getMessage());
        }
    }

    public void setMatchToEdit(MatchGame match) {
        this.matchToEdit = match;
        titleLabel.setText("Modifier match");
        submitBtn.setText("Modifier");

        if (match.getDateMatch() != null) {
            dateMatchPicker.setValue(match.getDateMatch().toLocalDateTime().toLocalDate());
        }
        score1Field.setText(match.getScoreTeam1() == null ? "" : String.valueOf(match.getScoreTeam1()));
        score2Field.setText(match.getScoreTeam2() == null ? "" : String.valueOf(match.getScoreTeam2()));
        if (match.getStatut() != null && !match.getStatut().isBlank()) {
            if (!statutCombo.getItems().contains(match.getStatut())) {
                statutCombo.getItems().add(match.getStatut());
            }
            statutCombo.setValue(match.getStatut());
        }
        selectOptionById(equipe1Combo, match.getEquipe1Id(), "Equipe");
        selectOptionById(equipe2Combo, match.getEquipe2Id(), "Equipe");
        selectOptionById(tournoiCombo, match.getTournoiId(), "Tournoi");
    }

    @FXML
    private void handleSubmit() {
        try {
            Timestamp dateMatch = parseTimestamp(dateMatchPicker.getValue());
            Integer score1 = parseNullableInt(score1Field.getText());
            Integer score2 = parseNullableInt(score2Field.getText());
            String statut = emptyToNull(statutCombo.getValue());
            int equipe1 = requireSelection(equipe1Combo, "Equipe 1").id;
            int equipe2 = requireSelection(equipe2Combo, "Equipe 2").id;
            int tournoi = requireSelection(tournoiCombo, "Tournoi").id;

            validateBusinessRules(dateMatch, score1, score2, statut, equipe1, equipe2);

            if (matchToEdit == null) {
                MatchGame match = new MatchGame(dateMatch, score1, score2, statut, equipe1, equipe2, tournoi);
                serviceMatchGame.ajouter(match);
                showSuccess("Match ajoute avec succes.");
            } else {
                matchToEdit.setDateMatch(dateMatch);
                matchToEdit.setScoreTeam1(score1);
                matchToEdit.setScoreTeam2(score2);
                matchToEdit.setStatut(statut);
                matchToEdit.setEquipe1Id(equipe1);
                matchToEdit.setEquipe2Id(equipe2);
                matchToEdit.setTournoiId(tournoi);
                serviceMatchGame.modifier(matchToEdit);
                showSuccess("Match modifie avec succes.");
            }

            closeWindow();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Erreur SQL : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void loadCombos() throws SQLException {
        equipe1Combo.setItems(FXCollections.observableArrayList());
        equipe2Combo.setItems(FXCollections.observableArrayList());
        tournoiCombo.setItems(FXCollections.observableArrayList());

        for (Equipe equipe : serviceEquipe.getAll()) {
            OptionItem option = new OptionItem(equipe.getId(), equipe.getNom());
            equipe1Combo.getItems().add(option);
            equipe2Combo.getItems().add(new OptionItem(equipe.getId(), equipe.getNom()));
        }

        for (Tournoi tournoi : serviceTournoi.getAll()) {
            tournoiCombo.getItems().add(new OptionItem(tournoi.getId(), tournoi.getNom()));
        }
    }

    private OptionItem requireSelection(ComboBox<OptionItem> combo, String fieldName) {
        OptionItem selected = combo.getValue();
        if (selected == null) {
            throw new IllegalArgumentException(fieldName + " obligatoire.");
        }
        return selected;
    }

    private void selectOptionById(ComboBox<OptionItem> combo, int id, String prefix) {
        for (OptionItem item : combo.getItems()) {
            if (item.id == id) {
                combo.setValue(item);
                return;
            }
        }
        OptionItem fallback = new OptionItem(id, prefix + " #" + id);
        combo.getItems().add(fallback);
        combo.setValue(fallback);
    }

    private Timestamp parseTimestamp(LocalDate selectedDate) {
        if (selectedDate == null) {
            throw new IllegalArgumentException("Date match obligatoire.");
        }
        return Timestamp.valueOf(selectedDate.atStartOfDay());
    }

    private Integer parseNullableInt(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            int score = Integer.parseInt(raw.trim());
            if (score < 0) {
                throw new IllegalArgumentException("Le score ne peut pas etre negatif.");
            }
            if (score > 99) {
                throw new IllegalArgumentException("Le score doit etre inferieur ou egal a 99.");
            }
            return score;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalArgumentException("Le score doit etre un entier.");
        }
    }

    private void validateBusinessRules(Timestamp dateMatch, Integer score1, Integer score2,
                                       String statut, int equipe1, int equipe2) {
        if (equipe1 == equipe2) {
            throw new IllegalArgumentException("Equipe 1 et Equipe 2 doivent etre differentes.");
        }

        if (statut == null || statut.isBlank()) {
            throw new IllegalArgumentException("Statut obligatoire.");
        }

        LocalDate matchDate = dateMatch.toLocalDateTime().toLocalDate();
        if (("Planifie".equalsIgnoreCase(statut) || "Scheduled".equalsIgnoreCase(statut))
            && matchDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Un match planifie ne peut pas avoir une date passee.");
        }

        if ((score1 == null) != (score2 == null)) {
            throw new IllegalArgumentException("Renseignez les deux scores ou aucun.");
        }

        if (("Termine".equalsIgnoreCase(statut) || "Finished".equalsIgnoreCase(statut))
                && (score1 == null || score2 == null)) {
            throw new IllegalArgumentException("Pour un match termine, les deux scores sont obligatoires.");
        }
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void closeWindow() {
        Stage stage = (Stage) dateMatchPicker.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: #27ae60;");
        messageLabel.setText(message);
    }

    private static class OptionItem {
        private final int id;
        private final String label;

        private OptionItem(int id, String label) {
            this.id = id;
            this.label = label == null || label.isBlank() ? ("#" + id) : label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
