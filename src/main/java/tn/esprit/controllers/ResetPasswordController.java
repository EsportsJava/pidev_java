package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tn.esprit.services.ResetPasswordService;

import java.io.IOException;
import java.sql.SQLException;

public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService = new ResetPasswordService();

    @FXML private Label emailLabel;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label codeError;
    @FXML private Label newPasswordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label messageLabel;
    @FXML private Button resetPasswordButton;

    private String email;

    @FXML
    public void initialize() {
        codeField.textProperty().addListener((obs, old, val) -> validateCode());
        newPasswordField.textProperty().addListener((obs, old, val) -> validateNewPassword());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> validateConfirmPassword());
    }

    public void setEmail(String email) {
        this.email = email;
        emailLabel.setText(email);
    }

    @FXML
    private void handleResetPassword() {
        boolean codeOk = validateCode();
        boolean passwordOk = validateNewPassword();
        boolean confirmOk = validateConfirmPassword();

        if (!codeOk || !passwordOk || !confirmOk) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Corrigez les erreurs avant de continuer.");
            return;
        }

        try {
            resetPasswordService.resetPassword(email, codeField.getText().trim(), newPasswordField.getText());
            messageLabel.setTextFill(Color.web("#27ae60"));
            messageLabel.setText("Mot de passe mis a jour. Redirection vers la connexion...");

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {
                }
                javafx.application.Platform.runLater(this::goToLogin);
            }).start();
        } catch (IllegalArgumentException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText(e.getMessage());
        } catch (SQLException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Erreur base de donnees : " + e.getMessage());
        }
    }

    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) resetPasswordButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Impossible d'ouvrir la page de connexion.");
        }
    }

    private boolean validateCode() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            setError(codeField, codeError, "Le code est obligatoire.");
            return false;
        }
        if (!code.matches("\\d{6}")) {
            setError(codeField, codeError, "Le code doit contenir 6 chiffres.");
            return false;
        }
        clearError(codeField, codeError);
        return true;
    }

    private boolean validateNewPassword() {
        String password = newPasswordField.getText();
        if (password.isEmpty()) {
            setError(newPasswordField, newPasswordError, "Le mot de passe est obligatoire.");
            return false;
        }
        if (password.length() < 6) {
            setError(newPasswordField, newPasswordError, "Au moins 6 caracteres.");
            return false;
        }
        clearError(newPasswordField, newPasswordError);
        return true;
    }

    private boolean validateConfirmPassword() {
        String confirmPassword = confirmPasswordField.getText();
        if (confirmPassword.isEmpty()) {
            setError(confirmPasswordField, confirmPasswordError, "Confirmez le mot de passe.");
            return false;
        }
        if (!confirmPassword.equals(newPasswordField.getText())) {
            setError(confirmPasswordField, confirmPasswordError, "Les mots de passe ne correspondent pas.");
            return false;
        }
        clearError(confirmPasswordField, confirmPasswordError);
        return true;
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
