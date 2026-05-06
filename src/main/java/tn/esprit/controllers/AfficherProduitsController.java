package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import tn.esprit.entities.Cart;
import tn.esprit.entities.Category;
import tn.esprit.entities.Product;
import tn.esprit.services.CartService;
import tn.esprit.services.CategoryService;
import tn.esprit.services.CurrencyService;
import tn.esprit.services.ProductService;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AfficherProduitsController implements Initializable {

    @FXML private FlowPane productsContainer;
    @FXML private VBox categoriesBox;
    @FXML private TextField searchField;
    @FXML private Button prevBtn, nextBtn;
    @FXML private Label pageLabel;
    @FXML private ComboBox<String> currencyCombo;
    @FXML private Label rateLabel;
    private CurrencyService currencyService = new CurrencyService();
    private String selectedCurrency = "TND";
    private static final int PRODUCTS_PER_PAGE = 6; // ← produits par page
    private int currentPage = 0;
    private List<Product> currentProducts;
    private ProductService productService = new ProductService();
    private CategoryService categoryService = new CategoryService();
    private List<Product> allProducts;
    private List<Category> allCategories;
    private CartService cartService = new CartService();
    private int currentUserId = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        allProducts = productService.getAllProducts();
        allCategories = categoryService.getAllCategories();
        loadCategoryButtons();
        displayProducts(allProducts);
        // ✅ Charger les devises
        currencyCombo.setItems(FXCollections.observableArrayList(
                "TND", "EUR", "USD", "GBP"
        ));
        currencyCombo.setValue("TND");

// ✅ Changer devise
        currencyCombo.setOnAction(e -> {
            selectedCurrency = currencyCombo.getValue();
            showPage(); // recharger les cartes
        });
    }

    private void loadCategoryButtons() {
        categoriesBox.getChildren().clear();
        for (Category cat : allCategories) {
            long count = allProducts.stream()
                    .filter(p -> p.getCategoryId() == cat.getId()).count();

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(8);

            Button btn = new Button(cat.getName());
            btn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btn, Priority.ALWAYS);
            btn.setStyle(
                    "-fx-background-color: #0d2137; -fx-text-fill: white;" +
                            "-fx-pref-height: 38px; -fx-cursor: hand;" +
                            "-fx-background-radius: 20; -fx-font-size: 13px;" +
                            "-fx-alignment: CENTER-LEFT; -fx-padding: 0 15;"
            );

            Label badge = new Label(String.valueOf(count));
            badge.setStyle(
                    "-fx-background-color: #7c3aed; -fx-text-fill: white;" +
                            "-fx-background-radius: 20; -fx-padding: 3 10;" +
                            "-fx-font-size: 12px;"
            );

            btn.setOnAction(e -> filterByCategory(cat.getId()));
            row.getChildren().addAll(btn, badge);
            categoriesBox.getChildren().add(row);
        }
    }

    private void displayProducts(List<Product> products) {
        this.currentProducts = products;
        this.currentPage = 0;
        showPage();
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 15;" +
                        "-fx-padding: 15;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);"
        );

        ImageView imageView = new ImageView();
        imageView.setFitWidth(190);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-cursor: hand;");
        imageView.setOnMouseClicked(event -> openProductDetails(p));

        File imgFile = new File("C:/wamp64/www/uploads/" + p.getImage());
        if (imgFile.exists()) {
            imageView.setImage(new Image(imgFile.toURI().toString()));
        }

        Label nameLabel = new Label(p.getName());
        nameLabel.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-cursor: hand;"
        );
        nameLabel.setWrapText(true);
        nameLabel.setOnMouseClicked(event -> openProductDetails(p));

        Label descLabel = new Label(p.getDescription());
        descLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        descLabel.setWrapText(true);

        Category cat = categoryService.getCategoryById(p.getCategoryId());
        Label catLabel = new Label("Category: " + (cat != null ? cat.getName() : "N/A"));
        catLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        String priceText;
        if (selectedCurrency.equals("TND")) {
            priceText = String.format("%.2f TND", p.getPrice());
        } else {
            double converted = currencyService.convertFromTND(p.getPrice(), selectedCurrency);
            priceText = converted > 0
                    ? String.format("%.2f %s", converted, selectedCurrency)
                    : String.format("%.2f TND", p.getPrice());
        }
        Label priceLabel = new Label(priceText);
        priceLabel.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;"
        );
        HBox.setHgrow(priceLabel, Priority.ALWAYS);

        Button addBtn = new Button("🛒 ADD");
        addBtn.setStyle(
                "-fx-background-color: #7c3aed; -fx-text-fill: white;" +
                        "-fx-cursor: hand; -fx-background-radius: 20;" +
                        "-fx-pref-height: 35px; -fx-font-size: 12px; -fx-padding: 0 15;"
        );

        addBtn.setOnMouseClicked(event -> event.consume());

        addBtn.setOnAction(e -> {
            System.out.println("=== ADD CLICKED ===");
            System.out.println("Product ID: " + p.getId());
            System.out.println("User ID: " + currentUserId);

            Cart cart = cartService.getOrCreateCart(currentUserId);
            System.out.println("Cart: " + (cart == null ? "NULL ❌" : "ID=" + cart.getId() + " ✅"));

            if (cart != null) {
                cartService.addToCart(cart.getId(), p.getId(), 1);
                System.out.println("addToCart appelé ✅");

                addBtn.setText("✅ Ajouté !");
                addBtn.setStyle(
                        "-fx-background-color: #27ae60; -fx-text-fill: white;" +
                                "-fx-cursor: hand; -fx-background-radius: 20;" +
                                "-fx-pref-height: 35px; -fx-font-size: 12px; -fx-padding: 0 15;"
                );

                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> {
                        addBtn.setText("🛒 ADD");
                        addBtn.setStyle(
                                "-fx-background-color: #7c3aed; -fx-text-fill: white;" +
                                        "-fx-cursor: hand; -fx-background-radius: 20;" +
                                        "-fx-pref-height: 35px; -fx-font-size: 12px; -fx-padding: 0 15;"
                        );
                    });
                }).start();
            }
        });

        footer.getChildren().addAll(priceLabel, addBtn);
        card.getChildren().addAll(imageView, nameLabel, descLabel, catLabel, footer);
        return card;
    }

    private void filterByCategory(int categoryId) {
        List<Product> filtered = allProducts.stream()
                .filter(p -> p.getCategoryId() == categoryId)
                .toList();
        displayProducts(filtered);
    }

    @FXML
    private void showAll() {
        displayProducts(allProducts);
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        if (keyword.isEmpty()) {
            displayProducts(allProducts);
            return;
        }
        List<Product> filtered = allProducts.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword)
                        || p.getDescription().toLowerCase().contains(keyword))
                .toList();
        displayProducts(filtered);
    }

    private void openProductDetails(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProductDetails.fxml"));
            Parent root = loader.load();

            ProductDetailsController controller = loader.getController();
            controller.setProduct(product);

            productsContainer.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ✅ Afficher la page courante
    private void showPage() {
        productsContainer.getChildren().clear();

        int totalPages = (int) Math.ceil((double) currentProducts.size() / PRODUCTS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        int start = currentPage * PRODUCTS_PER_PAGE;
        int end = Math.min(start + PRODUCTS_PER_PAGE, currentProducts.size());

        for (int i = start; i < end; i++) {
            productsContainer.getChildren().add(createProductCard(currentProducts.get(i)));
        }

        // Mise à jour label et boutons
        pageLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        prevBtn.setDisable(currentPage == 0);
        nextBtn.setDisable(currentPage >= totalPages - 1);
    }

    // ✅ Page suivante
    @FXML
    private void handleNext() {
        int totalPages = (int) Math.ceil((double) currentProducts.size() / PRODUCTS_PER_PAGE);
        if (currentPage < totalPages - 1) {
            currentPage++;
            showPage();
        }
    }

    // ✅ Page précédente
    @FXML
    private void handlePrevious() {
        if (currentPage > 0) {
            currentPage--;
            showPage();
        }
    }
}