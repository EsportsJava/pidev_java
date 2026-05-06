package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.esprit.entities.Product;

import java.io.File;

public class ProductDetailsController {

    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private Label stockLabel;
    @FXML private Label descriptionLabel;
    @FXML private ImageView imageView;

    public void setProduct(Product product) {
        nameLabel.setText(product.getName());
        priceLabel.setText(product.getPrice() + " TND");
        stockLabel.setText("Stock: " + product.getStock());
        descriptionLabel.setText(product.getDescription());

        File imgFile = new File("C:/wamp64/www/uploads/" + product.getImage());
        if (imgFile.exists()) {
            imageView.setImage(new Image(imgFile.toURI().toString()));
        }
    }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherProduits.fxml"));
            nameLabel.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}