package tn.esprit.controllers.equipe;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.entities.EquipeJoinRequest;
import tn.esprit.services.ServiceEquipe;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class PendingDemandesController implements Initializable {

    @FXML
    private TableView<EquipeJoinRequest> pendingTable;
    @FXML
    private TableColumn<EquipeJoinRequest, String> equipeCol;
    @FXML
    private TableColumn<EquipeJoinRequest, String> joueurCol;
    @FXML
    private TableColumn<EquipeJoinRequest, String> statutCol;
    @FXML
    private TableColumn<EquipeJoinRequest, java.sql.Timestamp> createdCol;
    @FXML
    private TextField motifField;
    @FXML
    private Label messageLabel;

    private final ServiceEquipe serviceEquipe = new ServiceEquipe();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        equipeCol.setCellValueFactory(new PropertyValueFactory<>("equipeNom"));
        joueurCol.setCellValueFactory(new PropertyValueFactory<>("joueurNom"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        loadPending();
    }

    @FXML
    private void handleRefresh() {
        loadPending();
    }

    @FXML
    private void handleAccept() {
        process(true);
    }

    @FXML
    private void handleReject() {
        process(false);
    }

    private void process(boolean accept) {
        EquipeJoinRequest selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selectionnez une demande.");
            return;
        }

        String motif = motifField.getText() == null ? "" : motifField.getText().trim();
        try {
            serviceEquipe.processJoinRequest(selected.getId(), accept, motif);
            showSuccess("Demande #" + selected.getId() + " " + (accept ? "acceptee" : "rejetee") + ".");
            loadPending();
        } catch (SQLException e) {
            showError("Traitement impossible: " + e.getMessage());
        }
    }

    private void loadPending() {
        try {
            List<EquipeJoinRequest> pending = serviceEquipe.getPendingRequestsForCurrentOwner();
            pendingTable.setItems(FXCollections.observableArrayList(pending));
            messageLabel.setText(pending.isEmpty() ? "Aucune demande pending." : "Demandes chargees.");
            messageLabel.setStyle("-fx-text-fill: #22c55e;");
        } catch (SQLException e) {
            showError("Erreur chargement pending: " + e.getMessage());
        }
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #ef4444;");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: #22c55e;");
        messageLabel.setText(message);
    }
}
