package tn.esprit.controllers;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.*;
import tn.esprit.services.*;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CheckoutController implements Initializable {

    @FXML private TextField firstNameField, lastNameField, emailField;
    @FXML private TextField addressField, cityField, postalCodeField, phoneField;
    @FXML private Label totalLabel, subtotalLabel, errorLabel;
    @FXML private VBox orderSummaryBox;
    @FXML private Button placeOrderBtn;

    private CartService cartService = new CartService();
    private ProductService productService = new ProductService();
    private OrderService orderService = new OrderService();
    private int currentUserId = 1;
    private double total = 0;
    private List<CartItem> cartItems;
    private Cart cart;

    private static final String STRIPE_KEY = "sk_test_51SyCQpLoei8vIeaG2Gs3Zsqdv6RrmlC0ihbhVF45DeiPV38SBYxRqEtBwhu1iGofLC4m9v6aeDzAJvkX5qPWxGM300YCM1X4nq";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Stripe.apiKey = STRIPE_KEY;
        loadOrderSummary();
    }

    private void loadOrderSummary() {
        orderSummaryBox.getChildren().clear();
        total = 0;

        cart = cartService.getOrCreateCart(currentUserId);
        if (cart == null) return;

        cartItems = cartService.getCartItems(cart.getId());

        for (CartItem item : cartItems) {
            Product p = productService.getProductById(item.getProductId());
            if (p == null) continue;

            double itemTotal = p.getPrice() * item.getQuantity();
            total += itemTotal;

            HBox row = new HBox();
            Label name = new Label(p.getName() + " x" + item.getQuantity());
            name.setStyle("-fx-text-fill: #333; -fx-font-size: 13px;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label price = new Label(String.format("%.2f TND", itemTotal));
            price.setStyle("-fx-text-fill: #333; -fx-font-size: 13px;");
            row.getChildren().addAll(name, spacer, price);
            orderSummaryBox.getChildren().add(row);
        }

        String totalStr = String.format("%.2f TND", total);
        totalLabel.setText(totalStr);
        subtotalLabel.setText(totalStr);
    }

    @FXML
    private void handlePlaceOrder() {
        // Validation
        if (firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty()
                || emailField.getText().isEmpty() || addressField.getText().isEmpty()
                || cityField.getText().isEmpty() || postalCodeField.getText().isEmpty()
                || phoneField.getText().isEmpty()) {
            errorLabel.setText("❌ Veuillez remplir tous les champs !");
            return;
        }

        placeOrderBtn.setText("⏳ Processing...");
        placeOrderBtn.setDisable(true);

        // Créer la commande en DB
        Order order = new Order();
        order.setFirstName(firstNameField.getText());
        order.setLastName(lastNameField.getText());
        order.setEmail(emailField.getText());
        order.setAddress(addressField.getText());
        order.setCity(cityField.getText());
        order.setPostalCode(postalCodeField.getText());
        order.setPhone(phoneField.getText());
        order.setTotalPrice(total);
        order.setUserId(currentUserId);

        Order savedOrder = orderService.createOrder(order);
        if (savedOrder == null) {
            errorLabel.setText("❌ Erreur lors de la commande !");
            placeOrderBtn.setDisable(false);
            return;
        }

        // ✅ Sauvegarder order items + réduire stock
        for (CartItem item : cartItems) {
            Product p = productService.getProductById(item.getProductId());
            if (p != null) {
                System.out.println("📦 Stock avant: " + p.getName() + " = " + p.getStock());

                orderService.addOrderItem(new OrderItem(
                        p.getName(), p.getPrice(), item.getQuantity(),
                        savedOrder.getId(), p.getId()
                ));

                orderService.reduceStock(p.getId(), item.getQuantity());

                Product updated = productService.getProductById(p.getId());
                System.out.println("📦 Stock après: " + updated.getName() + " = " + updated.getStock());
            }
        }

        // ✅ Créer session Stripe
        try {
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost/success?order=" + savedOrder.getId())
                    .setCancelUrl("http://localhost/cancel");

            for (CartItem item : cartItems) {
                Product p = productService.getProductById(item.getProductId());
                if (p != null) {
                    paramsBuilder.addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity((long) item.getQuantity())
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount((long) (p.getPrice() * 100))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(p.getName())
                                                                    .build()
                                                    ).build()
                                    ).build()
                    );
                }
            }

            Session session = Session.create(paramsBuilder.build());

            orderService.updateStripeSession(savedOrder.getId(), session.getId(), "");

            Desktop.getDesktop().browse(new URI(session.getUrl()));

            cartService.clearCart(cart.getId());

            errorLabel.setStyle("-fx-text-fill: #27ae60;");
            errorLabel.setText("✅ Redirecting to payment...");
            placeOrderBtn.setText("✅ Order Placed!");

        } catch (Exception e) {
            errorLabel.setText("❌ Stripe error: " + e.getMessage());
            placeOrderBtn.setText("PLACE ORDER");
            placeOrderBtn.setDisable(false);
            e.printStackTrace();
        }
    }
}