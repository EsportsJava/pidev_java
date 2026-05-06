package tn.esprit.services;
import java.sql.*;

import tn.esprit.entities.Cart;
import tn.esprit.entities.CartItem;
import java.util.List;
import java.util.ArrayList;
import tn.esprit.utils.MyDatabase;
public class CartService {
    private Connection conn;

    public CartService() {
        conn = MyDatabase.getInstance().getConnection();
    }

    public Cart getOrCreateCart(int userId) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM cart WHERE user_id = ?"
            );
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Cart cart = new Cart(userId);
                cart.setId(rs.getInt("id"));
                return cart;
            } else {
                PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO cart (user_id, created_at, updated_at) VALUES (?, NOW(), NOW())",
                        Statement.RETURN_GENERATED_KEYS
                );
                insert.setInt(1, userId);
                insert.executeUpdate();
                ResultSet keys = insert.getGeneratedKeys();
                Cart cart = new Cart(userId);
                if (keys.next()) cart.setId(keys.getInt(1));
                return cart;
            }
        } catch (SQLException e) {
            System.out.println("Erreur cart: " + e.getMessage());
            return null;
        }
    }

    // ✅ Ajouter produit au panier
    public void addToCart(int cartId, int productId, int quantity) {
        try {
            // Vérifier si le produit est déjà dans le panier
            PreparedStatement check = conn.prepareStatement(
                    "SELECT * FROM cart_item WHERE cart_id = ? AND product_id = ?"
            );
            check.setInt(1, cartId);
            check.setInt(2, productId);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                // ✅ Déjà présent → augmenter la quantité
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE cart_item SET quantity = quantity + ? WHERE cart_id = ? AND product_id = ?"
                );
                update.setInt(1, quantity);
                update.setInt(2, cartId);
                update.setInt(3, productId);
                update.executeUpdate();
            } else {
                // ✅ Nouveau → insérer
                PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO cart_item (quantity, cart_id, product_id) VALUES (?, ?, ?)"
                );
                insert.setInt(1, quantity);
                insert.setInt(2, cartId);
                insert.setInt(3, productId);
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Erreur addToCart: " + e.getMessage());
        }
    }
    // ✅ Récupérer les items du panier
    public List<CartItem> getCartItems(int cartId) {
        List<CartItem> items = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM cart_item WHERE cart_id = ?"
            );
            ps.setInt(1, cartId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CartItem item = new CartItem(
                        rs.getInt("quantity"),
                        rs.getInt("cart_id"),
                        rs.getInt("product_id")
                );
                item.setId(rs.getInt("id"));
                items.add(item);
            }
        } catch (SQLException e) {
            System.out.println("Erreur getCartItems: " + e.getMessage());
        }
        return items;
    }

    // ✅ Supprimer un item du panier
    public void removeFromCart(int cartId, int productId) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM cart_item WHERE cart_id = ? AND product_id = ?"
            );
            ps.setInt(1, cartId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur removeFromCart: " + e.getMessage());
        }
    }
    // Mettre à jour la quantité
    public void updateQuantity(int cartId, int productId, int quantity) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE cart_item SET quantity = ? WHERE cart_id = ? AND product_id = ?"
            );
            ps.setInt(1, quantity);
            ps.setInt(2, cartId);
            ps.setInt(3, productId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur updateQuantity: " + e.getMessage());
        }
    }

    // Vider le panier
    public void clearCart(int cartId) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM cart_item WHERE cart_id = ?"
            );
            ps.setInt(1, cartId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Erreur clearCart: " + e.getMessage());
        }
    }
}