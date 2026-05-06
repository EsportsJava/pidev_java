package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

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
}
