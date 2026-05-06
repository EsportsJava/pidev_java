package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.Jeu;
import tn.esprit.services.ServiceJeu;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class JeuxDashboardController implements Initializable {

    @FXML
    private TableView<Jeu> jeuxTable;
    @FXML
    private TableColumn<Jeu, String> nomCol;
    @FXML
    private TableColumn<Jeu, String> genreCol;
    @FXML
    private TableColumn<Jeu, String> plateformeCol;
    @FXML
    private TableColumn<Jeu, String> statutCol;
    @FXML
    private TableColumn<Jeu, String> actionsCol;
    @FXML
    private Label messageLabel;
    @FXML
    private Label totalJeuxLabel;
    @FXML
    private Label distinctGenresLabel;
    @FXML
    private Label distinctPlateformesLabel;

    private final ServiceJeu serviceJeu = new ServiceJeu();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadJeux();
    }

    private void setupColumns() {
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
        plateformeCol.setCellValueFactory(new PropertyValueFactory<>("plateforme"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑");
            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
            editBtn.setStyle("-fx-background-color: #6e5cff; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    Jeu j = getTableView().getItems().get(getIndex());
                    openEditWindow(j);
                });

                deleteBtn.setOnAction(e -> {
                    Jeu j = getTableView().getItems().get(getIndex());
                    confirmDelete(j);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadJeux() {
        try {
            List<Jeu> list = serviceJeu.getAll();
            jeuxTable.setItems(FXCollections.observableArrayList(list));
            updateStats(list);
            messageLabel.setText("");
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void updateStats(List<Jeu> list) {
        totalJeuxLabel.setText(String.valueOf(list.size()));
        Set<String> genres = new HashSet<>();
        Set<String> plats = new HashSet<>();
        for (Jeu j : list) {
            if (j.getGenre() != null && !j.getGenre().isBlank()) {
                genres.add(j.getGenre().trim());
            }
            if (j.getPlateforme() != null && !j.getPlateforme().isBlank()) {
                plats.add(j.getPlateforme().trim());
            }
        }
        distinctGenresLabel.setText(String.valueOf(genres.size()));
        distinctPlateformesLabel.setText(String.valueOf(plats.size()));
    }

    @FXML
    private void handleNewJeu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterJeu.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un jeu");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadJeux();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void openEditWindow(Jeu j) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterJeu.fxml"));
            Parent root = loader.load();
            AjouterJeuController controller = loader.getController();
            controller.setJeuToEdit(j);
            Stage stage = new Stage();
            stage.setTitle("Modifier le jeu");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadJeux();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void confirmDelete(Jeu j) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer le jeu « " + j.getNom() + " » ?");
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                try {
                    serviceJeu.supprimer(j.getId());
                    messageLabel.setStyle("-fx-text-fill: #27ae60;");
                    messageLabel.setText("Jeu supprimé.");
                    loadJeux();
                } catch (SQLException ex) {
                    messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                    messageLabel.setText("Suppression impossible : " + ex.getMessage());
                }
            }
        });
    }
}
