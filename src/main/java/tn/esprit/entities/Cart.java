package tn.esprit.entities;

public class Cart {
    private int id;
    private int userId;

    public Cart() {}
    public Cart(int userId) { this.userId = userId; }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}