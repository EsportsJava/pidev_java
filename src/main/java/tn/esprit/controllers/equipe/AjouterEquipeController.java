package tn.esprit.controllers.equipe;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.Equipe;
import tn.esprit.services.ServiceEquipe;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterEquipeController implements Initializable {

    @FXML
    private Label titleLabel;
    @FXML
    private TextField nomField;
    @FXML
    private TextField maxMembersField;
    @FXML
    private TextField logoField;
    @FXML
    private Label messageLabel;
    @FXML
    private Button submitBtn;

    private final ServiceEquipe serviceEquipe = new ServiceEquipe();
    private Equipe equipeToEdit;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // No-op
    }

    public void setEquipeToEdit(Equipe equipe) {
        this.equipeToEdit = equipe;
        titleLabel.setText("Modifier une equipe");
        submitBtn.setText("Modifier");

        nomField.setText(equipe.getNom());
        maxMembersField.setText(String.valueOf(equipe.getMaxMembers()));
        logoField.setText(equipe.getLogo());
    }

    @FXML
    private void handleSubmit() {
        String nom = nomField.getText() == null ? "" : nomField.getText().trim();
        String maxMembersRaw = maxMembersField.getText() == null ? "" : maxMembersField.getText().trim();
        String logo = logoField.getText() == null ? "" : logoField.getText().trim();

        if (nom.isEmpty()) {
            showError("Le nom est obligatoire.");
            return;
        }

        if (nom.length() < 3) {
            showError("Le nom doit contenir au moins 3 caracteres.");
            return;
        }

        if (nom.length() > 60) {
            showError("Le nom ne doit pas depasser 60 caracteres.");
            return;
        }

        int maxMembers;
        try {
            maxMembers = Integer.parseInt(maxMembersRaw);
        } catch (NumberFormatException e) {
            showError("Max members doit etre un nombre entier.");
            return;
        }

        if (maxMembers <= 0) {
            showError("Max members doit etre superieur a 0.");
            return;
        }

        if (maxMembers > 100) {
            showError("Max members doit etre inferieur ou egal a 100.");
            return;
        }

        if (logo.isEmpty()) {
            showError("Le logo est obligatoire.");
            return;
        }

        String logoLower = logo.toLowerCase();
        if (!(logoLower.endsWith(".png") || logoLower.endsWith(".jpg") || logoLower.endsWith(".jpeg")
                || logoLower.endsWith(".gif") || logoLower.endsWith(".webp"))) {
            showError("Le logo doit etre une image (.png, .jpg, .jpeg, .gif, .webp).");
            return;
        }

        try {
            if (equipeToEdit == null) {
                serviceEquipe.ajouter(new Equipe(nom, maxMembers, logo));
                showSuccess("Equipe ajoutee avec succes.");
            } else {
                equipeToEdit.setNom(nom);
                equipeToEdit.setMaxMembers(maxMembers);
                equipeToEdit.setLogo(logo);
                serviceEquipe.modifier(equipeToEdit);
                showSuccess("Equipe modifiee avec succes.");
            }

            closeWindow();
        } catch (SQLException e) {
            showError("Operation echouee : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    @FXML
    private void handleBrowseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un logo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File selected = chooser.showOpenDialog((Stage) logoField.getScene().getWindow());
        if (selected != null) {
            logoField.setText(selected.getAbsolutePath());
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) nomField.getScene().getWindow();
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
}
