package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class StatsDashboardController {

    @FXML
    private void handleLolRechercher(ActionEvent event) {
        openForm(event, StatsDetailController.Mode.LEAGUE);
    }

    @FXML
    private void handleValorantRechercher(ActionEvent event) {
        openForm(event, StatsDetailController.Mode.VALORANT);
    }

    @FXML
    private void handleSteamRechercher(ActionEvent event) {
        openForm(event, StatsDetailController.Mode.STEAM);
    }

    private void openForm(ActionEvent event, StatsDetailController.Mode mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/stats_form.fxml"));
            Parent root = loader.load();
            StatsDetailController ctrl = loader.getController();
            ctrl.initMode(mode);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("Stats – saisie compte");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
