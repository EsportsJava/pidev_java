package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.ServiceUser;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class UsersController implements Initializable {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idCol;
    @FXML private TableColumn<User, String> nomCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, Boolean> activeCol;
    @FXML private TableColumn<User, String> actionsCol;
    @FXML private Label messageLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label inactiveUsersLabel;

    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupColumns();
        loadUsers();
    }

    private void setupColumns() {
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("roles"));
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));

        activeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item ? "Active" : "Inactive"));
            }
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
            editBtn.setStyle("-fx-background-color: #6e5cff; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    openEditWindow(user);
                });

                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    try {
                        serviceUser.supprimer(user.getId());
                        loadUsers();
                        showMessage("Utilisateur supprime avec succes.", false);
                    } catch (SQLException ex) {
                        showMessage("Erreur lors de la suppression : " + ex.getMessage(), true);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadUsers() {
        try {
            List<User> users = serviceUser.getAll();
            usersTable.setItems(FXCollections.observableArrayList(users));
            updateStats(users);
        } catch (SQLException e) {
            showMessage("Erreur lors du chargement des utilisateurs : " + e.getMessage(), true);
        }
    }

    private void updateStats(List<User> users) {
        long activeUsers = users.stream().filter(User::isActive).count();
        totalUsersLabel.setText(String.valueOf(users.size()));
        activeUsersLabel.setText(String.valueOf(activeUsers));
        inactiveUsersLabel.setText(String.valueOf(users.size() - activeUsers));
    }

    @FXML
    private void handleNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterUser.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter Utilisateur");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadUsers();
        } catch (IOException e) {
            showMessage("Impossible d'ouvrir le formulaire utilisateur.", true);
        }
    }

    private void openEditWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterUser.fxml"));
            Parent root = loader.load();
            AjouterUserController controller = loader.getController();
            controller.setUserToEdit(user);
            Stage stage = new Stage();
            stage.setTitle("Modifier Utilisateur");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadUsers();
        } catch (IOException e) {
            showMessage("Impossible d'ouvrir la modification utilisateur.", true);
        }
    }

    private void showMessage(String message, boolean error) {
        messageLabel.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        messageLabel.setText(message);
    }
}
