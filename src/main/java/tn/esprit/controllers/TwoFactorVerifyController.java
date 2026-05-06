package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.TwoFactorService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;

public class TwoFactorVerifyController {

    @FXML private TextField codeField;
    @FXML private Label statusLabel;

    private final TwoFactorService twoFactorService = new TwoFactorService();

    // ── Verify on login ───────────────────────────────────────────────────────
    @FXML
    private void handleVerify(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            setStatus("❌ Session expired. Please login again.", "red");
            return;
        }

        String code = codeField.getText().trim();

        if (code.isEmpty() || code.length() != 6 || !code.matches("\\d+")) {
            setStatus("⚠ Please enter a valid 6-digit code.", "red");
            return;
        }

        boolean valid = twoFactorService.verifyCode(user.getGoogle2faSecret(), code);

        if (valid) {
            setStatus("✅ Verified! Logging in...", "#22aa44");
            new Thread(() -> {
                try { Thread.sleep(800); }
                catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(this::loadHomePage);
            }).start();
        } else {
            setStatus("❌ Invalid code. Please try again.", "red");
            codeField.clear();
        }
    }

    // ── Back to login ─────────────────────────────────────────────────────────
    @FXML
    private void handleBack(ActionEvent event) {
        SessionManager.clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
        } catch (IOException e) {
            setStatus("❌ Error: " + e.getMessage(), "red");
        }
    }

    private void loadHomePage() {
        try {
            User user = SessionManager.getCurrentUser();
            String fxml = "admin@gmail.com".equalsIgnoreCase(user.getEmail())
                    ? "/main.fxml" : "/home.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root, 980, 720));
            stage.setTitle("Esports Community");
        } catch (IOException e) {
            setStatus("❌ Error loading home: " + e.getMessage(), "red");
        }
    }

    private void setStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + ";");
    }
}