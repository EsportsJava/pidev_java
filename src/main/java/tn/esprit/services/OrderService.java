package tn.esprit.services;

import tn.esprit.entities.Order;
import tn.esprit.entities.OrderItem;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderService {
    private Connection conn;

    public OrderService() {
        conn = MyDatabase.getInstance().getConnection();
    }

    public Order createOrder(Order order) {
        try {
            String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `order` (order_number, total_price, status, first_name, last_name, " +
                            "email, address, city, postal_code, phone, user_id, created_at) " +
                            "VALUES (?, ?, 'pending', ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, orderNumber);
            ps.setDouble(2, order.getTotalPrice());
            ps.setString(3, order.getFirstName());
            ps.setString(4, order.getLastName());
            ps.setString(5, order.getEmail());
            ps.setString(6, order.getAddress());
            ps.setString(7, order.getCity());
            ps.setString(8, order.getPostalCode());
            ps.setString(9, order.getPhone());
            ps.setInt(10, order.getUserId());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                order.setId(keys.getInt(1));
                order.setOrderNumber(orderNumber);
            }
            return order;
        } catch (SQLException e) {
            System.out.println("Erreur createOrder: " + e.getMessage());
            return null;
        }
    }

    public void addOrderItem(OrderItem item) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO order_item (product_name, product_price, quantity, order_id, product_id) " +
                            "VALUES (?, ?, ?, ?, ?)"
            );
            ps.setString(1, item.getProductName());
            ps.setDouble(2, item.getProductPrice());
            ps.setInt(3, item.getQuantity());
            ps.setInt(4, item.getOrderId());
            ps.setInt(5, item.getProductId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur addOrderItem: " + e.getMessage());
        }
    }

    public void updateStripeSession(int orderId, String sessionId, String paymentIntentId) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE `order` SET stripe_session_id=?, stripe_payment_intent_id=?, status='paid', paid_at=NOW() WHERE id=?"
            );
            ps.setString(1, sessionId);
            ps.setString(2, paymentIntentId);
            ps.setInt(3, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur updateStripe: " + e.getMessage());
        }
    }

    public void updateOrderStatus(int orderId, String status) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE `order` SET status = ? WHERE id = ?"
            );
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
            System.out.println("✅ Order " + orderId + " status → " + status);
        } catch (SQLException e) {
            System.out.println("Erreur updateOrderStatus: " + e.getMessage());
        }
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM `order` ORDER BY created_at DESC"
            );
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Order o = new Order();
                o.setId(rs.getInt("id"));
                o.setOrderNumber(rs.getString("order_number"));
                o.setTotalPrice(rs.getDouble("total_price"));
                o.setStatus(rs.getString("status"));
                o.setFirstName(rs.getString("first_name"));
                o.setLastName(rs.getString("last_name"));
                o.setEmail(rs.getString("email"));
                o.setAddress(rs.getString("address"));
                o.setCity(rs.getString("city"));
                o.setPostalCode(rs.getString("postal_code"));
                o.setPhone(rs.getString("phone"));
                o.setCreatedAt(rs.getTimestamp("created_at"));
                o.setUserId(rs.getInt("user_id"));
                orders.add(o);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getAllOrders: " + e.getMessage());
        }
        return orders;
    }

    public void reduceStock(int productId, int quantity) {
        try {
            // ✅ Réduire le stock
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?"
            );
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            int rows = ps.executeUpdate();

            if (rows == 0) {
                System.out.println("❌ Stock insuffisant pour product ID: " + productId);
            } else {
                System.out.println("✅ Stock réduit pour product ID: " + productId + " (-" + quantity + ")");

                // ✅ Vérifier le nouveau stock
                PreparedStatement check = conn.prepareStatement(
                        "SELECT name, stock FROM product WHERE id = ?"
                );
                check.setInt(1, productId);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    String name = rs.getString("name");
                    int newStock = rs.getInt("stock");
                    System.out.println("📦 Nouveau stock: " + name + " = " + newStock);

                    // ✅ Envoyer email si stock faible
                    if (newStock < 5) {
                        System.out.println("⚠️ Stock faible détecté — envoi email !");
                        EmailService emailService = new EmailService();
                        new Thread(() ->
                                emailService.sendLowStockAlert(name, newStock, "saadamaryem776@gmail.com")
                        ).start();
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur reduceStock: " + e.getMessage());
        }
    }
}