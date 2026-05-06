package tn.esprit.controllers.equipe;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.services.ServiceEquipe;

import java.sql.SQLException;

public class DemandeAdhesionController {

    @FXML
    private TextField equipeIdField;
    @FXML
    private Label messageLabel;

    private final ServiceEquipe serviceEquipe = new ServiceEquipe();

    @FXML
    private void handleSubmit() {
        String raw = equipeIdField.getText() == null ? "" : equipeIdField.getText().trim();
        if (raw.isEmpty()) {
            showError("ID equipe obligatoire.");
            return;
        }

        int equipeId;
        try {
            equipeId = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            showError("ID equipe invalide.");
            return;
        }

        try {
            int requestId = serviceEquipe.createJoinRequest(equipeId);
            showSuccess("Demande envoyee. ID #" + requestId);
        } catch (SQLException e) {
            showError("Demande impossible: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) equipeIdField.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #ef4444;");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: #22c55e;");
        messageLabel.setText(message);
    }
}
