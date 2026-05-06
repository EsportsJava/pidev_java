package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.Jeu;
import tn.esprit.entities.Tournoi;
import tn.esprit.services.ServiceJeu;
import tn.esprit.services.ServiceTournoi;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class TournoiDashboardController implements Initializable {

    @FXML
    private TableView<Tournoi> tournoisTable;
    @FXML
    private TableColumn<Tournoi, String> nomCol;
    @FXML
    private TableColumn<Tournoi, String> jeuCol;
    @FXML
    private TableColumn<Tournoi, String> debutCol;
    @FXML
    private TableColumn<Tournoi, String> finCol;
    @FXML
    private TableColumn<Tournoi, String> statutCol;
    @FXML
    private TableColumn<Tournoi, String> actionsCol;
    @FXML
    private Label messageLabel;
    @FXML
    private Label totalTournoisLabel;

    private final ServiceTournoi serviceTournoi = new ServiceTournoi();
    private final ServiceJeu serviceJeu = new ServiceJeu();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private Map<Integer, String> jeuNoms = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadTournois();
    }

    private void refreshJeuNames() {
        jeuNoms.clear();
        try {
            for (Jeu j : serviceJeu.getAll()) {
                jeuNoms.put(j.getId(), j.getNom());
            }
        } catch (SQLException e) {
            showError("Impossible de charger les jeux : " + e.getMessage());
        }
    }

    private void setupColumns() {
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));

        jeuCol.setCellValueFactory(c ->
                new SimpleStringProperty(jeuNoms.getOrDefault(c.getValue().getJeuId(), "—")));

        debutCol.setCellValueFactory(c ->
                new SimpleStringProperty(fmt(c.getValue().getDateDebut())));

        finCol.setCellValueFactory(c ->
                new SimpleStringProperty(fmt(c.getValue().getDateFin())));

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox box = new HBox(8, editBtn, deleteBtn);

            {
            editBtn.setStyle("-fx-background-color: #6e5cff; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    Tournoi t = getTableView().getItems().get(getIndex());
                    openEditWindow(t);
                });

                deleteBtn.setOnAction(e -> {
                    Tournoi t = getTableView().getItems().get(getIndex());
                    confirmDelete(t);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private String fmt(java.util.Date d) {
        return d == null ? "" : dateFormat.format(d);
    }

    private void loadTournois() {
        refreshJeuNames();
        try {
            List<Tournoi> list = serviceTournoi.getAll();
            tournoisTable.setItems(FXCollections.observableArrayList(list));
            totalTournoisLabel.setText(String.valueOf(list.size()));
            tournoisTable.refresh();
            messageLabel.setText("");
        } catch (SQLException e) {
            showError("Impossible de charger les tournois : " + e.getMessage());
        }
    }

    @FXML
    private void handleNewTournoi() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterTournoi.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un tournoi");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadTournois();
        } catch (IOException e) {
            showError("Erreur d'ouverture du formulaire : " + e.getMessage());
        }
    }

    private void openEditWindow(Tournoi t) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterTournoi.fxml"));
            Parent root = loader.load();
            AjouterTournoiController controller = loader.getController();
            controller.setTournoiToEdit(t);
            Stage stage = new Stage();
            stage.setTitle("Modifier le tournoi");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadTournois();
        } catch (IOException e) {
            showError("Erreur d'ouverture du formulaire : " + e.getMessage());
        }
    }

    private void confirmDelete(Tournoi t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer le tournoi « " + t.getNom() + " » ?");
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                try {
                    serviceTournoi.supprimer(t.getId());
                    messageLabel.setStyle("-fx-text-fill: #4ade80;");
                    messageLabel.setText("Tournoi supprimé.");
                    loadTournois();
                } catch (SQLException ex) {
                    showError("Suppression impossible : " + ex.getMessage());
                }
            }
        });
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
