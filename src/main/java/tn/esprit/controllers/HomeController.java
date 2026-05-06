package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class HomeController {

    @FXML
    private Button homeBtn;

    @FXML
    private Button aboutBtn;

    @FXML
    private Button tournementBtn;

    @FXML
    private Label messageLabel;

    @FXML
    public void initialize() {
    }

    @FXML
    private void goToRegister(ActionEvent event) {
        try {
            loadScene(event, "/Register.fxml", "Register");
        } catch (IOException e) {
            showMessage("Unable to open register form.", true);
        }
    }


    private void loadScene(ActionEvent event, String fxml, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }

    private void showMessage(String msg, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setTextFill(isError ? Color.RED : Color.GREEN);
        } else {
            System.out.println(msg);
        }
    }

    // Call this from LoginController after successful login
    public static void show(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(
                HomeController.class.getResource("/Home.fxml")
        );
        stage.setScene(new Scene(root, 980, 720));
        stage.setTitle("Esports Community");
        stage.show();
    }
}