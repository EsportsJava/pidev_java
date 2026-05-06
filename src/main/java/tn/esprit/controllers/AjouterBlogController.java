package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.Blog;
import tn.esprit.services.ServiceBlog;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

public class AjouterBlogController implements Initializable {

    @FXML private TextField  titleField;
    @FXML private TextArea   categoryField;
    @FXML private TextField  contentField;
    @FXML private ImageView  imagePreview;
    @FXML private Label      titleLabel;
    @FXML private Label      titleError;
    @FXML private Label      categoryError;
    @FXML private Label      contentError;
    @FXML private Label      messageLabel;
    @FXML private Button     submitBtn;

    private static final String HTDOCS_PATH = "C:/xampp/htdocs/blog_images/";

    private final ServiceBlog serviceBlog = new ServiceBlog();
    private Blog   blogToEdit        = null;
    private String selectedImageName = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        titleField.textProperty().addListener((obs, old, val)    -> validateTitle());
        categoryField.textProperty().addListener((obs, old, val) -> validateCategory());
    }

    // ─── Validations ────────────────────────────────────────────────────

    private boolean validateTitle() {
        String val = titleField.getText().trim();
        if (val.isEmpty()) {
            setError(titleField, titleError, "Le titre est obligatoire.");
            return false;
        }
        if (val.length() < 3) {
            setError(titleField, titleError, "Minimum 3 caractères.");
            return false;
        }
        if (val.length() > 100) {
            setError(titleField, titleError, "Maximum 100 caractères.");
            return false;
        }
        clearError(titleField, titleError);
        return true;
    }

    private boolean validateCategory() {
        String val = categoryField.getText().trim();
        if (val.isEmpty()) {
            setCategoryError("La description est obligatoire.");
            return false;
        }
        if (val.length() < 10) {
            setCategoryError("Minimum 10 caractères.");
            return false;
        }
        if (val.length() > 500) {
            setCategoryError("Maximum 500 caractères.");
            return false;
        }
        clearCategoryError();
        return true;
    }

    private boolean validateImage() {
        if (selectedImageName.isEmpty() && contentField.getText().trim().isEmpty()) {
            setError(contentField, contentError, "Veuillez choisir une image.");
            return false;
        }
        clearError(contentField, contentError);
        return true;
    }

    // ─── Helpers visuels ────────────────────────────────────────────────

    private void setError(TextField field, Label errorLabel, String message) {
        field.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;" +
                "-fx-border-color: #ef4444; -fx-border-radius: 5;");
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError(TextField field, Label errorLabel) {
        field.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;" +
                "-fx-border-color: #27ae60; -fx-border-radius: 5;");
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void setCategoryError(String message) {
        categoryField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;" +
                "-fx-border-color: #ef4444; -fx-border-radius: 5;");
        categoryError.setText("⚠ " + message);
        categoryError.setVisible(true);
        categoryError.setManaged(true);
    }

    private void clearCategoryError() {
        categoryField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white;" +
                "-fx-border-color: #27ae60; -fx-border-radius: 5;");
        categoryError.setText("");
        categoryError.setVisible(false);
        categoryError.setManaged(false);
    }

    // ─── Choisir image ──────────────────────────────────────────────────

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(contentField.getScene().getWindow());

        if (selectedFile != null) {
            if (selectedFile.length() > 2 * 1024 * 1024) {
                setError(contentField, contentError, "Image trop lourde (max 2 MB).");
                return;
            }
            try {
                new File(HTDOCS_PATH).mkdirs();
                Path destination = Paths.get(HTDOCS_PATH + selectedFile.getName());
                Files.copy(selectedFile.toPath(), destination,
                        StandardCopyOption.REPLACE_EXISTING);

                selectedImageName = selectedFile.getName();
                contentField.setText(selectedImageName);

                imagePreview.setImage(new Image(selectedFile.toURI().toString()));
                imagePreview.setVisible(true);

                clearError(contentField, contentError);
                messageLabel.setStyle("-fx-text-fill: #4ade80;");
                messageLabel.setText("✅ Image sauvegardée dans htdocs !");

            } catch (Exception e) {
                setError(contentField, contentError, "Erreur : " + e.getMessage());
            }
        }
    }

    // ─── Submit ─────────────────────────────────────────────────────────

    @FXML
    private void handleSubmit() {
        boolean ok = validateTitle()
                & validateCategory()
                & validateImage();

        if (!ok) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("❌ Corrigez les erreurs avant de continuer.");
            return;
        }

        String title  = titleField.getText().trim();
        String desc   = categoryField.getText().trim();
        String image  = selectedImageName.isEmpty()
                ? contentField.getText().trim()
                : selectedImageName;

        if (blogToEdit == null) {
            serviceBlog.ajouter(new Blog(title, image, desc, null));
            messageLabel.setStyle("-fx-text-fill: #4ade80;");
            messageLabel.setText("✅ Blog ajouté !");
        } else {
            blogToEdit.setTitle(title);
            blogToEdit.setCategory(desc);
            blogToEdit.setContent(image);
            serviceBlog.modifier(blogToEdit);
            messageLabel.setStyle("-fx-text-fill: #4ade80;");
            messageLabel.setText("✅ Blog modifié !");
        }

        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() ->
                    ((Stage) titleField.getScene().getWindow()).close()
            );
        }).start();
    }

    @FXML
    private void handleCancel() {
        ((Stage) titleField.getScene().getWindow()).close();
    }

    // ─── Pré-remplir pour modification ──────────────────────────────────

    public void setBlogToEdit(Blog blog) {
        this.blogToEdit = blog;
        titleLabel.setText("✏️ Modifier le Blog");
        submitBtn.setText("💾 Modifier");

        titleField.setText(blog.getTitle());
        categoryField.setText(blog.getCategory());
        contentField.setText(blog.getContent());
        selectedImageName = blog.getContent();

        File imgFile = new File(HTDOCS_PATH + blog.getContent());
        if (imgFile.exists()) {
            imagePreview.setImage(new Image(imgFile.toURI().toString()));
            imagePreview.setVisible(true);
        }
    }
}