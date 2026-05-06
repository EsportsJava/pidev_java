package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tn.esprit.services.ResetPasswordService;

import java.io.IOException;
import java.sql.SQLException;

public class ForgotPasswordController {

    private final ResetPasswordService resetPasswordService = new ResetPasswordService();

    @FXML private TextField emailField;
    @FXML private Label emailError;
    @FXML private Label messageLabel;
    @FXML private Button sendCodeButton;

    @FXML
    public void initialize() {
        emailField.textProperty().addListener((obs, old, val) -> validateEmail());
    }

    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();
        if (!validateEmail()) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Corrigez l'email avant de continuer.");
            return;
        }

        try {
            resetPasswordService.requestPasswordReset(email);
            messageLabel.setTextFill(Color.web("#27ae60"));
            messageLabel.setText("Un code a ete envoye a votre adresse email.");
            openResetPasswordScreen(email);
        } catch (IllegalArgumentException | IllegalStateException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText(e.getMessage());
        } catch (SQLException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Erreur base de donnees : " + e.getMessage());
        } catch (IOException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Impossible d'ouvrir l'ecran de reinitialisation.");
        }
    }

    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) sendCodeButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Impossible d'ouvrir la page de connexion.");
        }
    }

    private boolean validateEmail() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            setError(emailField, emailError, "L'email est obligatoire.");
            return false;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            setError(emailField, emailError, "Veuillez entrer un email valide.");
            return false;
        }
        clearError(emailField, emailError);
        return true;
    }

    private void openResetPasswordScreen(String email) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResetPassword.fxml"));
        Parent root = loader.load();
        ResetPasswordController controller = loader.getController();
        controller.setEmail(email);

        Stage stage = (Stage) sendCodeButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Reset Password");
        stage.show();
    }

    private void setError(Control field, Label errorLabel, String message) {
        field.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        errorLabel.setText("! " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError(Control field, Label errorLabel) {
        field.setStyle("-fx-border-color: transparent transparent #27ae60 transparent; -fx-border-width: 0 0 1.5 0;");
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
