package tn.esprit.entities;

public class CartItem {
    private int id;
    private int quantity;
    private int cartId;
    private int productId;

    public CartItem() {}
    public CartItem(int quantity, int cartId, int productId) {
        this.quantity = quantity;
        this.cartId = cartId;
        this.productId = productId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getCartId() { return cartId; }
    public void setCartId(int cartId) { this.cartId = cartId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
}
