package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import tn.esprit.entities.Cart;
import tn.esprit.entities.CartItem;
import tn.esprit.entities.Product;
import tn.esprit.services.CartService;
import tn.esprit.services.ProductService;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CartController implements Initializable {

    @FXML private VBox cartItemsBox;
    @FXML private Label totalLabel;
    @FXML private Label subtotalLabel;

    private CartService cartService = new CartService();
    private ProductService productService = new ProductService();
    private int currentUserId = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadCart();
    }

    private void loadCart() {
        cartItemsBox.getChildren().clear();
        double total = 0;

        Cart cart = cartService.getOrCreateCart(currentUserId);
        if (cart == null) return;

        List<CartItem> items = cartService.getCartItems(cart.getId());

        for (CartItem item : items) {
            Product p = productService.getProductById(item.getProductId());
            if (p == null) continue;

            total += p.getPrice() * item.getQuantity();
            cartItemsBox.getChildren().add(createCartRow(cart, item, p));
        }

        String totalStr = String.format("%.2f TND", total);
        totalLabel.setText(totalStr);
        subtotalLabel.setText(totalStr);
    }

    private HBox createCartRow(Cart cart, CartItem item, Product p) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
                "-fx-background-color: #1a1a1a; -fx-background-radius: 12; -fx-padding: 20;"
        );

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-background-color: #2a2a2a;");

        File imgFile = new File("C:/wamp64/www/uploads/" + p.getImage());
        if (imgFile.exists()) {
            imageView.setImage(new Image(imgFile.toURI().toString()));
        }

        StackPane imgContainer = new StackPane(imageView);
        imgContainer.setMinWidth(100);
        imgContainer.setMinHeight(100);
        imgContainer.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 8;");

        // Info
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(p.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label catLabel = new Label(p.getDescription());
        catLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");

        // Delete button
        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #e53935;" +
                        "-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0;"
        );
        deleteBtn.setOnAction(e -> {
            cartService.removeFromCart(cart.getId(), p.getId());
            loadCart();
        });

        info.getChildren().addAll(nameLabel, catLabel, deleteBtn);

        // Price
        Label priceLabel = new Label(String.format("$%.2f", p.getPrice()));
        priceLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        priceLabel.setMinWidth(80);

        // Quantity controls
        HBox qtyBox = new HBox(0);
        qtyBox.setAlignment(Pos.CENTER);
        qtyBox.setStyle(
                "-fx-background-color: #2a2a2a; -fx-background-radius: 25; -fx-padding: 5 15;"
        );

        Button minusBtn = new Button("−");
        minusBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white;" +
                        "-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0 10;"
        );

        Label qtyLabel = new Label(String.valueOf(item.getQuantity()));
        qtyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-padding: 0 10;");

        Button plusBtn = new Button("+");
        plusBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white;" +
                        "-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 0 10;"
        );

        minusBtn.setOnAction(e -> {
            int newQty = item.getQuantity() - 1;
            if (newQty <= 0) {
                cartService.removeFromCart(cart.getId(), p.getId());
            } else {
                cartService.updateQuantity(cart.getId(), p.getId(), newQty);
            }
            loadCart();
        });

        plusBtn.setOnAction(e -> {
            cartService.updateQuantity(cart.getId(), p.getId(), item.getQuantity() + 1);
            loadCart();
        });

        qtyBox.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

        row.getChildren().addAll(imgContainer, info, priceLabel, qtyBox);
        return row;
    }

    @FXML
    private void handleOrder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/checkout.fxml"));
            Parent root = loader.load();
            cartItemsBox.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        totalLabel.setText("✅ Commande passée !");
        subtotalLabel.setText("✅");
    }

    @FXML
    private void handleClearCart() {
        Cart cart = cartService.getOrCreateCart(currentUserId);
        if (cart != null) {
            cartService.clearCart(cart.getId());
            loadCart();
        }
    }
}