package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.controllers.AjouterCategorieController;
import tn.esprit.entities.Category;
import tn.esprit.services.CategoryService;
import tn.esprit.services.ProductService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CategoriesController implements Initializable {

    @FXML private TableView<Category> categoriesTable;
    @FXML private TableColumn<Category, Integer> idCol;
    @FXML private TableColumn<Category, String> nameCol, actionsCol;
    @FXML private Label messageLabel, totalCatLabel, withProductsLabel, emptyCatLabel;

    private CategoryService categoryService = new CategoryService();
    private ProductService productService = new ProductService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadCategories();
    }

    private void setupColumns() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑");
            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
            editBtn.setStyle("-fx-background-color: #6e5cff; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    Category c = getTableView().getItems().get(getIndex());
                    openEditWindow(c);
                });

                deleteBtn.setOnAction(e -> {
                    Category c = getTableView().getItems().get(getIndex());
                    categoryService.deleteCategory(c.getId());
                    loadCategories();
                    messageLabel.setStyle("-fx-text-fill: #27ae60;");
                    messageLabel.setText("✅ Catégorie supprimée !");
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadCategories() {
        List<Category> list = categoryService.getAllCategories();
        categoriesTable.setItems(FXCollections.observableArrayList(list));
        updateStats(list);
    }

    private void updateStats(List<Category> list) {
        totalCatLabel.setText(String.valueOf(list.size()));
        long withProducts = list.stream()
                .filter(c -> productService.getAllProducts().stream()
                        .anyMatch(p -> p.getCategoryId() == c.getId()))
                .count();
        withProductsLabel.setText(String.valueOf(withProducts));
        emptyCatLabel.setText(String.valueOf(list.size() - withProducts));
    }

    @FXML
    private void handleNewCategory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterCategorie.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter Catégorie");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCategories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openEditWindow(Category c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterCategorie.fxml"));
            Parent root = loader.load();
            AjouterCategorieController controller = loader.getController();
            controller.setCategoryToEdit(c);
            Stage stage = new Stage();
            stage.setTitle("Modifier Catégorie");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCategories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
