package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Category;
import tn.esprit.services.CategoryService;

import java.net.URL;
import java.util.ResourceBundle;

public class AjouterCategorieController implements Initializable {

    @FXML private TextField nameField;
    @FXML private Label messageLabel, titleLabel;
    @FXML private Button submitBtn;

    private CategoryService categoryService = new CategoryService();
    private Category categoryToEdit = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setCategoryToEdit(Category c) {
        this.categoryToEdit = c;
        titleLabel.setText("✏️ Modifier la Catégorie");
        submitBtn.setText("💾 Modifier");
        nameField.setText(c.getName());
    }

    @FXML
    private void handleSubmit() {
        if (nameField.getText().isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("❌ Le nom est obligatoire !");
            return;
        }

        if (categoryToEdit == null) {
            categoryService.addCategory(new Category(nameField.getText()));
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setText("✅ Catégorie ajoutée !");
        } else {
            categoryToEdit.setName(nameField.getText());
            categoryService.updateCategory(categoryToEdit);
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setText("✅ Catégorie modifiée !");
        }

        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() ->
                    ((Stage) nameField.getScene().getWindow()).close()
            );
        }).start();
    }

    @FXML
    private void handleCancel() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}