package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.ServiceUser;
import tn.esprit.services.TwoFactorService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;

public class TwoFactorSetupController {

    @FXML private ImageView qrCodeImageView;
    @FXML private TextField secretKeyField;
    @FXML private TextField verificationCodeField;
    @FXML private Label statusLabel;

    private final TwoFactorService twoFactorService = new TwoFactorService();
    private final ServiceUser serviceUser = new ServiceUser();
    private String currentSecret;

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        generateQR();
    }

    private void generateQR() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            setStatus("❌ No user session found.", "red");
            return;
        }

        // Generate new secret
        currentSecret = twoFactorService.generateSecret();
        secretKeyField.setText(currentSecret);

        // Generate and display QR
        Image qrImage = twoFactorService.generateQRCodeImage(user.getEmail(), currentSecret);
        if (qrImage != null) {
            qrCodeImageView.setImage(qrImage);
        } else {
            setStatus("❌ Failed to generate QR code.", "red");
        }
    }

    // ── Copy Secret ───────────────────────────────────────────────────────────
    @FXML
    private void handleCopySecret(ActionEvent event) {
        if (currentSecret == null) return;
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(currentSecret);
        clipboard.setContent(content);
        setStatus("✅ Secret key copied to clipboard!", "#22aa44");
    }

    // ── Verify & Enable ───────────────────────────────────────────────────────
    @FXML
    private void handleVerify(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            setStatus("❌ No user session found.", "red");
            return;
        }

        String code = verificationCodeField.getText().trim();

        if (code.isEmpty()) {
            setStatus("⚠ Please enter the verification code.", "red");
            return;
        }

        if (code.length() != 6 || !code.matches("\\d+")) {
            setStatus("⚠ Code must be exactly 6 digits.", "red");
            return;
        }

        boolean enabled = twoFactorService.enable2FA(user, currentSecret, code);

        if (enabled) {
            try {
                serviceUser.modifier(user);
                SessionManager.setCurrentUser(user);

                setStatus("✅ 2FA enabled successfully! Redirecting...", "#22aa44");

                // Navigate back after short delay
                new Thread(() -> {
                    try { Thread.sleep(1500); }
                    catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(this::goToProfile);
                }).start();

            } catch (SQLException e) {
                setStatus("❌ Database error: " + e.getMessage(), "red");
            }
        } else {
            setStatus("❌ Invalid code. Please try again.", "red");
            verificationCodeField.clear();
        }
    }

    // ── Back ──────────────────────────────────────────────────────────────────
    @FXML
    private void handleBack(ActionEvent event) {
        goToProfile();
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    private void goToProfile() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/ProfileUser.fxml")
            );
            Stage stage = (Stage) qrCodeImageView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Profile");
        } catch (IOException e) {
            setStatus("❌ Error navigating back: " + e.getMessage(), "red");
        }
    }

    private void setStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + "; -fx-alignment: center;");
    }
}