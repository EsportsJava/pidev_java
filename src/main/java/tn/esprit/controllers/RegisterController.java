package tn.esprit.controllers;

import javafx.event.ActionEvent;
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
import tn.esprit.entities.User;
import tn.esprit.services.GoogleOAuthService;
import tn.esprit.services.ServiceUser;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;

public class RegisterController {

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Label nomError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Button registerButton;

    @FXML
    private void initialize() {
        nomField.textProperty().addListener((obs, old, val) -> validateNom());
        emailField.textProperty().addListener((obs, old, val) -> validateEmail());
        passwordField.textProperty().addListener((obs, old, val) -> validatePassword());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> validateConfirmPassword());
    }

    @FXML
    private void handleRegister() {
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // bech nValidi les champs
        boolean nomOk = validateNom();
        boolean emailOk = validateEmail();
        boolean passwordOk = validatePassword();
        boolean confirmPasswordOk = validateConfirmPassword();

        if (!nomOk || !emailOk || !passwordOk || !confirmPasswordOk) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Corrigez les erreurs avant de continuer.");
            return;
        }

        try {
            if (serviceUser.emailExists(email)) {
                setError(emailField, emailError, "Cet email est déjà utilisé.");
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText(" Cet email est déjà utilisé.");
                return;
            }

            User user = new User(
                    email,
                    "[\"ROLE_USER\"]",
                    password,
                    nom,
                    true,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null,
                    false
            );

            serviceUser.ajouter(user);
            messageLabel.setTextFill(Color.web("#27ae60"));
            messageLabel.setText("Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
            clearFields();

            // Fermer après 2 secondes et retourner au login
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> {
                    try {
                        Stage stage = (Stage) registerButton.getScene().getWindow();
                        Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                        stage.setScene(new Scene(root));
                        stage.setTitle("Login");
                        stage.show();
                    } catch (IOException ignored) {}
                });
            }).start();

        } catch (SQLException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText(" Erreur base de données : " + e.getMessage());
        }
    }

    private boolean validateNom() {
        String nom = nomField.getText().trim();

        if (nom.isEmpty()) {
            setError(nomField, nomError, "Le nom est obligatoire.");
            return false;
        }

        if (nom.length() < 3) {
            setError(nomField, nomError, "Le nom doit contenir au moins 3 caractères.");
            return false;
        }

        if (nom.length() > 100) {
            setError(nomField, nomError, "Le nom ne peut pas dépasser 100 caractères.");
            return false;
        }

        clearError(nomField, nomError);
        return true;
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

    private boolean validatePassword() {
        String password = passwordField.getText();

        if (password.isEmpty()) {
            setError(passwordField, passwordError, "Le mot de passe est obligatoire.");
            return false;
        }

        if (password.length() < 6) {
            setError(passwordField, passwordError, "Le mot de passe doit contenir au moins 6 caractères.");
            return false;
        }

        if (password.length() > 50) {
            setError(passwordField, passwordError, "Le mot de passe ne peut pas dépasser 50 caractères.");
            return false;
        }

        clearError(passwordField, passwordError);
        return true;
    }

    private boolean validateConfirmPassword() {
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (confirmPassword.isEmpty()) {
            setError(confirmPasswordField, confirmPasswordError, "Veuillez confirmer le mot de passe.");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            setError(confirmPasswordField, confirmPasswordError, "Les mots de passe ne correspondent pas.");
            return false;
        }

        clearError(confirmPasswordField, confirmPasswordError);
        return true;
    }

    private void setError(Control field, Label errorLabel, String message) {
        field.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError(Control field, Label errorLabel) {
        field.setStyle("-fx-border-color: transparent transparent #27ae60 transparent; -fx-border-width: 0 0 1.5 0;");
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void goToLogin(ActionEvent event) {
        try {
            loadScene(event, "/Login.fxml", "Login");
        } catch (IOException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText(" Impossible d'ouvrir le formulaire de connexion.");
        }
    }

    private void loadScene(ActionEvent event, String resourcePath, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(resourcePath));
        Stage stage = (Stage) ((Control) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }

    private void clearFields() {
        nomField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }

    @FXML
    private void handleGoogleSignUp() {
        try {
            // Google OAuth credentials from user input
            String clientId = "460245401741-m94mns2ae0675cvo0fsiiahrr9qqbagu.apps.googleusercontent.com";
            String clientSecret = "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";

            GoogleOAuthService oauthService = new GoogleOAuthService(clientId, clientSecret);
            User googleUser = oauthService.authenticateAndGetUser();

            // Check if user already exists
            User existingUser = serviceUser.findByGoogleOauthId(googleUser.getGoogleOauthId());
            if (existingUser != null) {
                // User exists, redirect to login
                messageLabel.setTextFill(Color.web("#5b4cdf"));
                messageLabel.setText("Un compte Google existe déjà. Redirection vers la connexion...");
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> {
                        try {
                            loadScene(null, "/Login.fxml", "Login");
                        } catch (IOException ignored) {}
                    });
                }).start();
            } else {
                // Check if email is already registered with regular account
                if (serviceUser.emailExists(googleUser.getEmail())) {
                    messageLabel.setTextFill(Color.RED);
                    messageLabel.setText("Un compte avec cet email existe déjà. Veuillez vous connecter normalement.");
                    return;
                }

                // Create new user
                User newUser = serviceUser.createOAuthUser(googleUser);
                SessionManager.setCurrentUser(newUser);
                messageLabel.setTextFill(Color.web("#27ae60"));
                messageLabel.setText("Compte créé avec succès ! Bienvenue, " + newUser.getNom() + "!");

                // Redirect to home after 2 seconds
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> {
                        try {
                            loadHomePage();
                        } catch (IOException ignored) {}
                    });
                }).start();
            }

        } catch (SQLException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Erreur base de données : " + e.getMessage());
        } catch (Exception e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Erreur lors de l'authentification Google : " + e.getMessage());
        }
    }

    private void loadHomePage() throws IOException {
        User user = SessionManager.getCurrentUser();
        String fxml = "/home.fxml";
        if ("admin@gmail.com".equalsIgnoreCase(user.getEmail())) {
            fxml = "/main.fxml";
        }
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) registerButton.getScene().getWindow();
        stage.setScene(new Scene(root, 980, 720));
        stage.setTitle("Esports Community");
        stage.show();
    }
}
