package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.Category;
import tn.esprit.entities.Product;
import tn.esprit.services.CategoryService;
import tn.esprit.services.HuggingFaceService;
import tn.esprit.services.ProductService;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ResourceBundle;
import javafx.concurrent.Task;

public class AjouterProduitController implements Initializable {

    @FXML private TextField nameField, descriptionField, priceField, stockField, imageField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private Label messageLabel, titleLabel;
    @FXML private Label nameError, descError, priceError, stockError, imageError, categoryError;
    @FXML private Button submitBtn;
    @FXML private ImageView imagePreview;

    private ProductService productService = new ProductService();
    private CategoryService categoryService = new CategoryService();
    private Product productToEdit = null;
    private List<Category> categories;
    private String selectedImagePath = "";
    // ✅ Après
    private HuggingFaceService huggingFaceService = new HuggingFaceService();
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        categories = categoryService.getAllCategories();
        categoryCombo.setItems(FXCollections.observableArrayList(categories));

        categoryCombo.setCellFactory(lv -> new ListCell<>() {
            protected void updateItem(Category c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "" : c.getName());
            }
        });
        categoryCombo.setButtonCell(new ListCell<>() {
            protected void updateItem(Category c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "" : c.getName());
            }
        });


        nameField.textProperty().addListener((obs, old, val) -> validateName());
        descriptionField.textProperty().addListener((obs, old, val) -> validateDescription());
        priceField.textProperty().addListener((obs, old, val) -> validatePrice());
        stockField.textProperty().addListener((obs, old, val) -> validateStock());
        categoryCombo.valueProperty().addListener((obs, old, val) -> validateCategory());
    }


    private boolean validateName() {
        String val = nameField.getText().trim();
        if (val.isEmpty()) {
            setError(nameField, nameError, "Le nom est obligatoire.");
            return false;
        }
        if (val.length() < 3) {
            setError(nameField, nameError, "Minimum 3 caractères.");
            return false;
        }
        if (val.length() > 100) {
            setError(nameField, nameError, "Maximum 100 caractères.");
            return false;
        }

        clearError(nameField, nameError);
        return true;
    }

    private boolean validateDescription() {
        String val = descriptionField.getText().trim();
        if (val.length() > 500) {
            setError(descriptionField, descError, "Maximum 500 caractères.");
            return false;
        }
        clearError(descriptionField, descError);
        return true;
    }

    private boolean validatePrice() {
        String val = priceField.getText().trim();
        if (val.isEmpty()) {
            setError(priceField, priceError, "Le prix est obligatoire.");
            return false;
        }
        try {
            double price = Double.parseDouble(val);
            if (price <= 0) {
                setError(priceField, priceError, "Le prix doit être supérieur à 0.");
                return false;
            }
            if (price > 99999) {
                setError(priceField, priceError, "Le prix est trop élevé (max 99999).");
                return false;
            }
        } catch (NumberFormatException e) {
            setError(priceField, priceError, "Le prix doit être un nombre (ex: 19.99).");
            return false;
        }
        clearError(priceField, priceError);
        return true;
    }

    private boolean validateStock() {
        String val = stockField.getText().trim();
        if (val.isEmpty()) {
            setError(stockField, stockError, "Le stock est obligatoire.");
            return false;
        }
        try {
            int stock = Integer.parseInt(val);
            if (stock < 0) {
                setError(stockField, stockError, "Le stock ne peut pas être négatif.");
                return false;
            }
            if (stock > 99999) {
                setError(stockField, stockError, "Stock trop élevé (max 99999).");
                return false;
            }
        } catch (NumberFormatException e) {
            setError(stockField, stockError, "Le stock doit être un nombre entier.");
            return false;
        }
        clearError(stockField, stockError);
        return true;
    }

    private boolean validateImage() {
        if (selectedImagePath.isEmpty() && imageField.getText().trim().isEmpty()) {
            setError(imageField, imageError, "Veuillez choisir une image.");
            return false;
        }
        clearError(imageField, imageError);
        return true;
    }

    private boolean validateCategory() {
        if (categoryCombo.getValue() == null) {
            categoryError.setText("Veuillez sélectionner une catégorie.");
            categoryError.setVisible(true);
            categoryCombo.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
            return false;
        }
        categoryError.setText("");
        categoryError.setVisible(false);
        categoryCombo.setStyle("-fx-border-color: #27ae60; -fx-border-radius: 5;");
        return true;
    }



    private void setError(TextField field, Label errorLabel, String message) {
        field.setStyle("-fx-border-color: red; -fx-border-radius: 5;");
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError(TextField field, Label errorLabel) {
        field.setStyle("-fx-border-color: #27ae60; -fx-border-radius: 5;");
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void handleGenerateDescription() {
        String productName = nameField.getText().trim();

        if (productName.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("❌ Entrez d'abord le nom du produit !");
            return;
        }

        // Loading
        descriptionField.setText("⏳ Génération en cours...");
        descriptionField.setDisable(true);
        messageLabel.setStyle("-fx-text-fill: #94a3b8;");
        messageLabel.setText("🤖 Grok AI génère la description...");

        // Thread séparé pour ne pas bloquer l'UI
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return huggingFaceService.generateDescription(productName);
            }
        };

        task.setOnSucceeded(e -> {
            String description = task.getValue();
            if (description != null) {
                descriptionField.setText(description.trim());
                messageLabel.setStyle("-fx-text-fill: #27ae60;");
                messageLabel.setText("✅ Description générée par Grok AI !");
            } else {
                descriptionField.setText("");
                messageLabel.setStyle("-fx-text-fill: #f87171;");
                messageLabel.setText("❌ Erreur génération !");
            }
            descriptionField.setDisable(false);
        });

        task.setOnFailed(e -> {
            descriptionField.setText("");
            descriptionField.setDisable(false);
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("❌ Erreur : " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(imageField.getScene().getWindow());

        if (selectedFile != null) {

            if (selectedFile.length() > 2 * 1024 * 1024) {
                setError(imageField, imageError, "Image trop lourde (max 2 MB).");
                return;
            }

            try {
                String destDir = "C:/wamp64/www/uploads/";
                new File(destDir).mkdirs();
                Path destination = Paths.get(destDir + selectedFile.getName());
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                selectedImagePath = selectedFile.getName();
                imageField.setText(selectedImagePath);

                Image image = new Image(selectedFile.toURI().toString());
                imagePreview.setImage(image);
                imagePreview.setVisible(true);

                clearError(imageField, imageError);
                messageLabel.setStyle("-fx-text-fill: #27ae60;");
                messageLabel.setText("✅ Image sauvegardée !");

            } catch (Exception e) {
                setError(imageField, imageError, "Erreur : " + e.getMessage());
            }
        }
    }



    @FXML
    private void handleSubmit() {

        boolean ok = validateName()
                & validateDescription()
                & validatePrice()
                & validateStock()
                & validateImage()
                & validateCategory();

        if (!ok) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(" Corrigez les erreurs avant de continuer.");
            return;
        }

        String name     = nameField.getText().trim();
        String desc     = descriptionField.getText().trim();
        double price    = Double.parseDouble(priceField.getText().trim());
        int stock       = Integer.parseInt(stockField.getText().trim());
        String image    = selectedImagePath.isEmpty() ? imageField.getText().trim() : selectedImagePath;
        int catId       = categoryCombo.getValue().getId();

        if (productToEdit == null) {
            productService.addProduct(new Product(name, desc, price, stock, image, catId));
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setText("✅ Produit ajouté !");
        } else {
            productToEdit.setName(name);
            productToEdit.setDescription(desc);
            productToEdit.setPrice(price);
            productToEdit.setStock(stock);
            productToEdit.setImage(image);
            productToEdit.setCategoryId(catId);
            productService.updateProduct(productToEdit);
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setText("✅ Produit modifié !");
        }

        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() ->
                    ((Stage) nameField.getScene().getWindow()).close()
            );
        }).start();
    }

    @FXML
    private void handleCancel() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    public void setProductToEdit(Product p) {
        this.productToEdit = p;
        titleLabel.setText("✏️ Modifier le Produit");
        submitBtn.setText("💾 Modifier");
        nameField.setText(p.getName());
        descriptionField.setText(p.getDescription());
        priceField.setText(String.valueOf(p.getPrice()));
        stockField.setText(String.valueOf(p.getStock()));
        imageField.setText(p.getImage());
        selectedImagePath = p.getImage();

        File imgFile = new File("C:/wamp64/www/uploads/" + p.getImage());
        if (imgFile.exists()) {
            imagePreview.setImage(new Image(imgFile.toURI().toString()));
            imagePreview.setVisible(true);
        }

        categories.stream()
                .filter(c -> c.getId() == p.getCategoryId())
                .findFirst()
                .ifPresent(c -> categoryCombo.setValue(c));
    }

}