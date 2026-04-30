package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Page publique « Jeux » (navbar) : catalogue en lecture seule.
 */
public class JeuxCatalogController implements Initializable {

    @FXML
    private JeuxPanelController jeuxPanelController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (jeuxPanelController != null) {
            jeuxPanelController.setReadOnly(true);
        }
    }

    @FXML
    private void openKpiPage(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/jeuxKpi.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("KPIs Jeux");
            stage.show();
        } catch (IOException e) {
            // optional no-op in catalogue view
        }
    }
}
