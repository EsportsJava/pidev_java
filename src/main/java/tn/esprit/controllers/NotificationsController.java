package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.entities.User;
import tn.esprit.services.NotificationService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class NotificationsController implements Initializable {

    @FXML
    private VBox listBox;
    @FXML
    private Label statusLabel;

    private final NotificationService notificationService = new NotificationService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        User user = SessionManager.getCurrentUser();
        if (user == null || user.getId() <= 0) {
            statusLabel.setText("Connectez-vous pour voir vos notifications.");
            return;
        }
        try {
            notificationService.generateTournamentReminderNotifications(user);
            JSONArray arr = notificationService.listByUser(user.getId());
            listBox.getChildren().clear();
            if (arr.isEmpty()) {
                statusLabel.setText("Aucune notification.");
                return;
            }
            statusLabel.setText(arr.length() + " notification(s).");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject n = arr.getJSONObject(i);
                listBox.getChildren().add(renderItem(n));
            }
            notificationService.markAllRead(user.getId());
        } catch (SQLException e) {
            statusLabel.setText("Erreur notifications : " + e.getMessage());
        }
    }

    private VBox renderItem(JSONObject n) {
        Label title = new Label(n.optString("title", "Notification"));
        title.setStyle("-fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold;");
        Label body = new Label(n.optString("body", ""));
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13;");
        Label date = new Label(n.optString("createdAt", ""));
        date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");

        VBox box = new VBox(6, title, body, date);
        box.setStyle("-fx-background-color: #111a33; -fx-border-color: #26365f; -fx-border-radius: 12; "
                + "-fx-background-radius: 12; -fx-padding: 12;");
        return box;
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/home.fxml"));
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1280, 720));
        stage.setTitle("Home");
        stage.show();
    }
}

