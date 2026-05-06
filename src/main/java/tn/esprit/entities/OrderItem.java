package tn.esprit.entities;

public class OrderItem {
    private int id;
    private String productName;
    private double productPrice;
    private int quantity;
    private int orderId;
    private int productId;

    public OrderItem() {}
    public OrderItem(String productName, double productPrice, int quantity, int orderId, int productId) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.orderId = orderId;
        this.productId = productId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
}