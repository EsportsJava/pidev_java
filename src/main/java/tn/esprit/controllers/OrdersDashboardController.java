package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.esprit.entities.Order;
import tn.esprit.services.OrderService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OrdersDashboardController implements Initializable {

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> colId, colOrderNumber, colName;
    @FXML private TableColumn<Order, String> colEmail, colTotal, colStatus;
    @FXML private TableColumn<Order, String> colCity, colPhone, colDate;
    @FXML private Label totalOrdersLabel, paidOrdersLabel, pendingOrdersLabel, revenueLabel;
    @FXML private TextField searchField;

    private OrderService orderService = new OrderService();
    private ObservableList<Order> allOrders = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadOrders();
        setupSearch();
    }

    private void setupTable() {
        colOrderNumber.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOrderNumber()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFirstName() + " " + d.getValue().getLastName()
        ));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f TND", d.getValue().getTotalPrice())
        ));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colCity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCity()));
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone()));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toString() : ""
        ));

        // ✅ Colorier le status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status.toUpperCase());
                    switch (status.toLowerCase()) {
                        case "paid" ->
                                setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        case "pending" ->
                                setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                        case "cancelled" ->
                                setStyle("-fx-text-fill: #e53935; -fx-font-weight: bold;");
                        default ->
                                setStyle("-fx-text-fill: #94a3b8;");
                    }
                }
            }
        });

        // Style table
        ordersTable.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white;"
        );
    }

    private void loadOrders() {
        List<Order> orders = orderService.getAllOrders();
        allOrders.setAll(orders);
        ordersTable.setItems(allOrders);
        updateStats(orders);
    }

    private void updateStats(List<Order> orders) {
        totalOrdersLabel.setText(String.valueOf(orders.size()));

        long paid = orders.stream().filter(o -> "paid".equals(o.getStatus())).count();
        long pending = orders.stream().filter(o -> "pending".equals(o.getStatus())).count();
        double revenue = orders.stream()
                .filter(o -> "paid".equals(o.getStatus()))
                .mapToDouble(Order::getTotalPrice).sum();

        paidOrdersLabel.setText(String.valueOf(paid));
        pendingOrdersLabel.setText(String.valueOf(pending));
        revenueLabel.setText(String.format("%.2f TND", revenue));
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String keyword = newVal.toLowerCase();
            if (keyword.isEmpty()) {
                ordersTable.setItems(allOrders);
            } else {
                ObservableList<Order> filtered = FXCollections.observableArrayList(
                        allOrders.stream()
                                .filter(o ->
                                        o.getOrderNumber().toLowerCase().contains(keyword) ||
                                                o.getFirstName().toLowerCase().contains(keyword) ||
                                                o.getLastName().toLowerCase().contains(keyword) ||
                                                o.getEmail().toLowerCase().contains(keyword) ||
                                                o.getStatus().toLowerCase().contains(keyword)
                                ).collect(Collectors.toList())
                );
                ordersTable.setItems(filtered);
            }
        });
    }
}