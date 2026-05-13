package tn.esprit.controllers.equipe;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.User;
import tn.esprit.services.ServiceEquipe;
import tn.esprit.services.ServiceUser;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ManageMembersController implements Initializable {

    @FXML
    private Label titleLabel;
    @FXML
    private Label equipeInfoLabel;
    @FXML
    private ComboBox<User> userComboBox;
    @FXML
    private Button addMemberBtn;
    @FXML
    private TableView<User> membersTable;
    @FXML
    private TableColumn<User, String> nomCol;
    @FXML
    private TableColumn<User, String> emailCol;
    @FXML
    private TableColumn<User, String> actionsCol;
    @FXML
    private Label messageLabel;

    private Equipe equipe;
    private final ServiceEquipe serviceEquipe = new ServiceEquipe();
    private final ServiceUser serviceUser = new ServiceUser();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadUsers();
    }

    public void setEquipe(Equipe equipe) {
        this.equipe = equipe;
        equipeInfoLabel.setText("Équipe: " + equipe.getNom() + " (Max: " + equipe.getMaxMembers() + " membres)");
        loadMembers();
    }

    private void setupTable() {
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        // ComboBox cell factory
        userComboBox.setCellFactory(lv -> new javafx.scene.control.ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item != null ? item.getNom() + " (" + item.getEmail() + ")" : null));
            }
        });
        userComboBox.setButtonCell(new javafx.scene.control.ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item != null ? item.getNom() + " (" + item.getEmail() + ")" : null));
            }
        });

        // Actions column
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Retirer");
            private final HBox box = new HBox(5, removeBtn);

            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                removeBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; "
                    + "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px;");
                removeBtn.setOnAction(e -> {
                    User member = getTableView().getItems().get(getIndex());
                    handleRemoveMember(member);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            }
        });
    }

    private void loadUsers() {
        try {
            List<User> allUsers = serviceUser.getAll();
            // Filter out admins and current members
            List<User> availableUsers = allUsers.stream()
                .filter(u -> !u.getRoles().toLowerCase().contains("admin"))
                .filter(u -> equipe.getMembers() == null || 
                        equipe.getMembers().stream().noneMatch(m -> m.getId() == u.getId()))
                .collect(Collectors.toList());
            userComboBox.setItems(FXCollections.observableArrayList(availableUsers));
        } catch (SQLException e) {
            showError("Erreur chargement utilisateurs: " + e.getMessage());
        }
    }

    private void loadMembers() {
        if (equipe.getMembers() != null) {
            membersTable.setItems(FXCollections.observableArrayList(equipe.getMembers()));
        }
    }

    @FXML
    private void handleAddMember() {
        User selectedUser = userComboBox.getValue();
        if (selectedUser == null) {
            showError("Veuillez sélectionner un utilisateur.");
            return;
        }

        try {
            serviceEquipe.addUserToEquipe(equipe.getId(), selectedUser.getId());
            showSuccess("Membre ajouté avec succès.");
            // Refresh data
            equipe.getMembers().add(selectedUser);
            loadUsers(); // Refresh available users
            loadMembers(); // Refresh table
        } catch (SQLException e) {
            showError("Erreur ajout membre: " + e.getMessage());
        }
    }

    private void handleRemoveMember(User member) {
        try {
            serviceEquipe.removeMemberFromEquipe(equipe.getId(), member.getId());
            showSuccess("Membre retiré avec succès.");
            equipe.getMembers().remove(member);
            loadUsers(); // Refresh available users
            loadMembers(); // Refresh table
        } catch (SQLException e) {
            showError("Erreur retrait membre: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: #27ae60;");
        messageLabel.setText(message);
    }
}