package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Control;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.ServiceUser;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterUserController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField rolesField;
    @FXML private CheckBox activeCheckBox;
    @FXML private Label messageLabel;
    @FXML private Button submitBtn;
    @FXML private Label nomError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label rolesError;

    private final ServiceUser serviceUser = new ServiceUser();
    private User userToEdit;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        rolesField.setText("[\"ROLE_USER\"]");
        activeCheckBox.setSelected(true);

        nomField.textProperty().addListener((obs, old, val) -> validateNom());
        emailField.textProperty().addListener((obs, old, val) -> validateEmail());
        passwordField.textProperty().addListener((obs, old, val) -> validatePassword());
        rolesField.textProperty().addListener((obs, old, val) -> validateRoles());
    }

    public void setUserToEdit(User user) {
        this.userToEdit = user;
        titleLabel.setText("Modifier Utilisateur");
        submitBtn.setText("💾 Modifier");
        nomField.setText(user.getNom());
        emailField.setText(user.getEmail());
        passwordField.setText(user.getPassword());
        rolesField.setText(user.getRoles());
        activeCheckBox.setSelected(user.isActive());
    }

    @FXML
    private void handleSubmit() {
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String roles = rolesField.getText().trim();

        boolean nomOk = validateNom();
        boolean emailOk = validateEmail();
        boolean passwordOk = validatePassword();
        boolean rolesOk = validateRoles();

        if (!nomOk || !emailOk || !passwordOk || !rolesOk) {
            showMessage(" Corrigez les erreurs avant de continuer.", true);
            return;
        }

        try {
            if (userToEdit == null && serviceUser.emailExists(email)) {
                setError(emailField, emailError, "Cet email existe déjà.");
                showMessage("Cet email existe déjà.", true);
                return;
            }

            if (userToEdit != null && serviceUser.emailExistsForOtherUser(email, userToEdit.getId())) {
                setError(emailField, emailError, "Cet email est déjà utilisé par un autre utilisateur.");
                showMessage(" Cet email est déjà utilisé par un autre utilisateur.", true);
                return;
            }

            if (userToEdit == null) {
                User user = new User(
                        email,
                        roles,
                        password,
                        nom,
                        activeCheckBox.isSelected(),
                        null,
                        false,
                        null,
                        null,
                        null,
                        null,
                        false
                );
                serviceUser.ajouter(user);
                showMessage(" Utilisateur ajouté avec succès.", false);
            } else {
                userToEdit.setNom(nom);
                userToEdit.setEmail(email);
                userToEdit.setPassword(password);
                userToEdit.setRoles(roles);
                userToEdit.setActive(activeCheckBox.isSelected());
                serviceUser.modifier(userToEdit);
                showMessage(" Utilisateur modifié avec succès.", false);
            }

            closeAfterDelay();
        } catch (SQLException e) {
            showMessage(" Erreur base de données : " + e.getMessage(), true);
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

    private boolean validateRoles() {
        String roles = rolesField.getText().trim();

        if (roles.isEmpty()) {
            setError(rolesField, rolesError, "Les rôles sont obligatoires.");
            return false;
        }

        if (!roles.startsWith("[") || !roles.endsWith("]")) {
            setError(rolesField, rolesError, "Le format des rôles doit être : [\"ROLE_...\"]");
            return false;
        }

        clearError(rolesField, rolesError);
        return true;
    }

    private void setError(Control field, Label errorLabel, String message) {
        field.setStyle("-fx-border-color: #ef4444; -fx-border-radius: 5;");
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError(Control field, Label errorLabel) {
        field.setStyle("-fx-border-color: #10b981; -fx-border-radius: 5;");
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleCancel() {
        ((Stage) nomField.getScene().getWindow()).close();
    }

    private void showMessage(String message, boolean error) {
        messageLabel.setStyle(error ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #10b981;");
        messageLabel.setText(message);
    }

    private void closeAfterDelay() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> ((Stage) nomField.getScene().getWindow()).close());
        });
        thread.setDaemon(true);
        thread.start();
    }
}
