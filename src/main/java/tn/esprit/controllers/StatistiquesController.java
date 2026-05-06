package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import tn.esprit.entities.Category;
import tn.esprit.entities.Product;
import tn.esprit.services.CategoryService;
import tn.esprit.services.ProductService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class StatistiquesController implements Initializable {

    @FXML private BarChart<String, Number> barChart;
    @FXML private PieChart pieChart;
    @FXML private LineChart<String, Number> lineChart;
    @FXML private Label totalProduitsLabel, totalCategoriesLabel, prixMoyenLabel, stockTotalLabel;

    private ProductService productService = new ProductService();
    private CategoryService categoryService = new CategoryService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        List<Product> products = productService.getAllProducts();
        List<Category> categories = categoryService.getAllCategories();

        loadStats(products, categories);
        loadBarChart(products, categories);
        loadPieChart(products, categories);
        loadLineChart(products);
    }

    // ✅ Cartes statistiques
    private void loadStats(List<Product> products, List<Category> categories) {
        totalProduitsLabel.setText(String.valueOf(products.size()));
        totalCategoriesLabel.setText(String.valueOf(categories.size()));

        double avgPrice = products.stream()
                .mapToDouble(Product::getPrice)
                .average().orElse(0);
        prixMoyenLabel.setText(String.format("%.0f", avgPrice));

        int totalStock = products.stream()
                .mapToInt(Product::getStock)
                .sum();
        stockTotalLabel.setText(String.valueOf(totalStock));
    }

    // ✅ Graphique Barres — Produits par catégorie
    private void loadBarChart(List<Product> products, List<Category> categories) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de produits");

        for (Category cat : categories) {
            long count = products.stream()
                    .filter(p -> p.getCategoryId() == cat.getId())
                    .count();
            series.getData().add(new XYChart.Data<>(cat.getName(), count));
        }

        barChart.getData().add(series);
        barChart.setLegendVisible(false);
        barChart.setStyle("-fx-background-color: #1e293b;");
    }

    // ✅ Graphique Camembert — Répartition par catégorie
    private void loadPieChart(List<Product> products, List<Category> categories) {
        for (Category cat : categories) {
            long count = products.stream()
                    .filter(p -> p.getCategoryId() == cat.getId())
                    .count();
            if (count > 0) {
                pieChart.getData().add(
                        new PieChart.Data(cat.getName() + " (" + count + ")", count)
                );
            }
        }
        pieChart.setLegendVisible(true);
        pieChart.setStyle("-fx-background-color: #1e293b;");
    }

    // ✅ Graphique Ligne — Prix des produits
    private void loadLineChart(List<Product> products) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Prix (TND)");

        for (Product p : products) {
            series.getData().add(new XYChart.Data<>(p.getName(), p.getPrice()));
        }

        lineChart.getData().add(series);
        lineChart.setLegendVisible(false);
        lineChart.setStyle("-fx-background-color: #1e293b;");
    }
}