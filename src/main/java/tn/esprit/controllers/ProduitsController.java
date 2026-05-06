package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
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
import tn.esprit.entities.Category;
import tn.esprit.entities.Product;
import tn.esprit.services.CategoryService;
import tn.esprit.services.ProductService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ProduitsController implements Initializable {

    @FXML private TableView<Product> produitsTable;
    @FXML private TableColumn<Product, Integer> idCol, stockCol;
    @FXML private TableColumn<Product, String> nameCol, descCol, catCol, actionsCol;
    @FXML private TableColumn<Product, Double> priceCol;
    @FXML private Label messageLabel, totalProductsLabel, inStockLabel, outOfStockLabel, avgPriceLabel;
    @FXML private TextField searchField;

    private ProductService productService = new ProductService();
    private CategoryService categoryService = new CategoryService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadProducts();
    }

    private void setupColumns() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));


        catCol.setCellValueFactory(cellData -> {
            Category cat = categoryService.getCategoryById(
                    cellData.getValue().getCategoryId()
            );
            return new SimpleStringProperty(cat != null ? cat.getName() : "N/A");
        });


        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑");
            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
            editBtn.setStyle("-fx-background-color: #6e5cff; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    openEditWindow(p);
                });

                deleteBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    productService.deleteProduct(p.getId());
                    loadProducts();
                    messageLabel.setStyle("-fx-text-fill: #27ae60;");
                    messageLabel.setText("✅ Produit supprimé !");
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadProducts() {
        List<Product> list = productService.getAllProducts();
        produitsTable.setItems(FXCollections.observableArrayList(list));
        updateStats(list);
    }

    private void updateStats(List<Product> list) {
        totalProductsLabel.setText(String.valueOf(list.size()));
        long inStock = list.stream().filter(p -> p.getStock() > 0).count();
        long outStock = list.stream().filter(p -> p.getStock() == 0).count();
        double avg = list.stream().mapToDouble(Product::getPrice).average().orElse(0);
        inStockLabel.setText(String.valueOf(inStock));
        outOfStockLabel.setText(String.valueOf(outStock));
        avgPriceLabel.setText(String.format("%.0f", avg));
    }

    @FXML
    private void handleNewProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterProduit.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter Produit");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadProducts();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openEditWindow(Product p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterProduit.fxml"));
            Parent root = loader.load();
            AjouterProduitController controller = loader.getController();
            controller.setProductToEdit(p);
            Stage stage = new Stage();
            stage.setTitle("Modifier Produit");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadProducts();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        List<Product> filtered = productService.getAllProducts().stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword))
                .toList();
        produitsTable.setItems(FXCollections.observableArrayList(filtered));
    }
}
