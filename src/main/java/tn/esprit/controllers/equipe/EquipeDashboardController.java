package tn.esprit.controllers.equipe;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.Equipe;
import tn.esprit.services.PandaScoreService;
import tn.esprit.services.ServiceEquipe;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class EquipeDashboardController implements Initializable {

    @FXML
    private TableView<Equipe> equipeTable;
    @FXML
    private TableColumn<Equipe, String> nomCol;
    @FXML
    private TableColumn<Equipe, Integer> maxMembersCol;
    @FXML
    private TableColumn<Equipe, String> logoCol;
    @FXML
    private TableColumn<Equipe, String> actionsCol;
    @FXML
    private Label messageLabel;
    @FXML
    private Label totalEquipesLabel;
    @FXML
    private Label totalMembersCapLabel;
    @FXML
    private Label avgMembersLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private TextField apiCountField;
    @FXML
    private Button generateApiBtn;
    @FXML
    private Label apiStatusLabel;

    private final ServiceEquipe serviceEquipe = new ServiceEquipe();
    private final List<Equipe> allEquipes = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupSearchAndSort();
        loadEquipes();
    }

    private void setupSearchAndSort() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "ID croissant",
                "ID decroissant",
                "Nom A-Z",
                "Nom Z-A",
                "Max members croissant",
                "Max members decroissant"
        ));
        sortCombo.getSelectionModel().select("ID decroissant");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSort());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSort());
    }

    private void setupColumns() {
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        maxMembersCol.setCellValueFactory(new PropertyValueFactory<>("maxMembers"));
        logoCol.setCellValueFactory(new PropertyValueFactory<>("logo"));

        // Style table programmatically
        equipeTable.setStyle("-fx-background-color: #111b3e; -fx-background-radius: 10;"
            + "-fx-border-color: rgba(124,58,237,0.15); -fx-border-radius: 10;");
        equipeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Row factory for dark themed rows with hover
        equipeTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Equipe> row = new javafx.scene.control.TableRow<>() {
                @Override
                protected void updateItem(Equipe item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("-fx-background-color: #111b3e;");
                    } else {
                        setStyle(getIndex() % 2 == 0
                            ? "-fx-background-color: #111b3e;"
                            : "-fx-background-color: #0f1835;");
                    }
                }
            };
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) row.setStyle("-fx-background-color: rgba(124,58,237,0.12);");
            });
            row.setOnMouseExited(e -> {
                if (!row.isEmpty()) {
                    row.setStyle(row.getIndex() % 2 == 0
                        ? "-fx-background-color: #111b3e;"
                        : "-fx-background-color: #0f1835;");
                }
            });
            return row;
        });

        // Styled name column
        nomCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: 700;");
                    setGraphic(lbl);
                    setText(null);
                }
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Styled maxMembers column
        maxMembersCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(String.valueOf(item));
                    badge.setStyle("-fx-background-color: rgba(124,58,237,0.18); -fx-text-fill: #c4b5fd;"
                        + "-fx-padding: 4 14; -fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: 700;");
                    setGraphic(badge);
                    setText(null);
                }
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Styled logo column (truncated with tooltip)
        logoCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String display = item.length() > 35 ? "..." + item.substring(item.length() - 32) : item;
                    Label lbl = new Label(display);
                    lbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                    lbl.setMaxWidth(250);
                    setGraphic(lbl);
                    setText(null);
                    setTooltip(new javafx.scene.control.Tooltip(item));
                }
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Actions column — theme-matched buttons
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏");
            private final Button deleteBtn = new Button("🗑");
            private final HBox box = new HBox(8, editBtn, deleteBtn);

            private final String editDefault = "-fx-background-color: linear-gradient(to right, #7c3aed, #6d28d9);"
                + "-fx-text-fill: #e9d5ff; -fx-font-size: 14px;"
                + "-fx-background-radius: 10; -fx-padding: 5 14;"
                + "-fx-cursor: hand; -fx-min-height: 30; -fx-min-width: 38;"
                + "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.35), 6, 0, 0, 2);";
            private final String editHover = "-fx-background-color: linear-gradient(to right, #8b5cf6, #7c3aed);"
                + "-fx-text-fill: white; -fx-font-size: 14px;"
                + "-fx-background-radius: 10; -fx-padding: 5 14;"
                + "-fx-cursor: hand; -fx-min-height: 30; -fx-min-width: 38;"
                + "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.55), 10, 0, 0, 3);";
            private final String deleteDefault = "-fx-background-color: linear-gradient(to right, #be185d, #9f1239);"
                + "-fx-text-fill: #fecdd3; -fx-font-size: 14px;"
                + "-fx-background-radius: 10; -fx-padding: 5 14;"
                + "-fx-cursor: hand; -fx-min-height: 30; -fx-min-width: 38;"
                + "-fx-effect: dropshadow(gaussian, rgba(190,24,93,0.35), 6, 0, 0, 2);";
            private final String deleteHover = "-fx-background-color: linear-gradient(to right, #e11d48, #be185d);"
                + "-fx-text-fill: white; -fx-font-size: 14px;"
                + "-fx-background-radius: 10; -fx-padding: 5 14;"
                + "-fx-cursor: hand; -fx-min-height: 30; -fx-min-width: 38;"
                + "-fx-effect: dropshadow(gaussian, rgba(225,29,72,0.55), 10, 0, 0, 3);";

            {
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                editBtn.setStyle(editDefault);
                deleteBtn.setStyle(deleteDefault);

                editBtn.setOnMouseEntered(e -> editBtn.setStyle(editHover));
                editBtn.setOnMouseExited(e -> editBtn.setStyle(editDefault));
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(deleteHover));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(deleteDefault));

                editBtn.setOnAction(e -> {
                    Equipe equipe = getTableView().getItems().get(getIndex());
                    openEditWindow(equipe);
                });
                deleteBtn.setOnAction(e -> {
                    Equipe equipe = getTableView().getItems().get(getIndex());
                    confirmDelete(equipe);
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

    private void loadEquipes() {
        try {
            List<Equipe> list = serviceEquipe.getAll();
            allEquipes.clear();
            allEquipes.addAll(list);
            applyFilterAndSort();
            computeStats();
            messageLabel.setText("");
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Erreur chargement equipes : " + e.getMessage());
        }
    }

    private void applyFilterAndSort() {
        String keyword = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        List<Equipe> filtered = new ArrayList<>();
        for (Equipe equipe : allEquipes) {
            if (keyword.isEmpty()
                    || String.valueOf(equipe.getId()).contains(keyword)
                    || safeLower(equipe.getNom()).contains(keyword)
                    || safeLower(equipe.getLogo()).contains(keyword)
                    || String.valueOf(equipe.getMaxMembers()).contains(keyword)) {
                filtered.add(equipe);
            }
        }

        Comparator<Equipe> comparator = getSortComparator(sortCombo == null ? null : sortCombo.getValue());
        filtered.sort(comparator);
        equipeTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private Comparator<Equipe> getSortComparator(String sortValue) {
        if ("ID croissant".equals(sortValue)) {
            return Comparator.comparingInt(Equipe::getId);
        }
        if ("Nom A-Z".equals(sortValue)) {
            return Comparator.comparing(e -> safeLower(e.getNom()));
        }
        if ("Nom Z-A".equals(sortValue)) {
            return Comparator.comparing((Equipe e) -> safeLower(e.getNom())).reversed();
        }
        if ("Max members croissant".equals(sortValue)) {
            return Comparator.comparingInt(Equipe::getMaxMembers);
        }
        if ("Max members decroissant".equals(sortValue)) {
            return Comparator.comparingInt(Equipe::getMaxMembers).reversed();
        }
        return Comparator.comparingInt(Equipe::getId).reversed();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void computeStats() {
        int total = allEquipes.size();
        totalEquipesLabel.setText(String.valueOf(total));

        int totalCap = 0;
        for (Equipe eq : allEquipes) {
            totalCap += eq.getMaxMembers();
        }
        totalMembersCapLabel.setText(String.valueOf(totalCap));
        avgMembersLabel.setText(total > 0 ? String.valueOf(totalCap / total) : "0");
    }

    @FXML
    private void handleNewEquipe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/equipe/ajouterEquipe.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter une equipe");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadEquipes();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    private void openEditWindow(Equipe equipe) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/equipe/ajouterEquipe.fxml"));
            Parent root = loader.load();
            AjouterEquipeController controller = loader.getController();
            controller.setEquipeToEdit(equipe);
            Stage stage = new Stage();
            stage.setTitle("Modifier une equipe");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadEquipes();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    private void confirmDelete(Equipe equipe) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer l'equipe " + equipe.getNom() + " ?");
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                try {
                    serviceEquipe.supprimer(equipe.getId());
                    messageLabel.setStyle("-fx-text-fill: #27ae60;");
                    messageLabel.setText("Equipe supprimee avec succes.");
                    loadEquipes();
                } catch (SQLException e) {
                    messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                    messageLabel.setText("Suppression impossible : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleGenerateFromApi() {
        String countText = apiCountField.getText();
        if (countText == null || countText.trim().isEmpty()) {
            apiStatusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px; -fx-font-weight: 700;");
            apiStatusLabel.setText("Entrez un nombre.");
            return;
        }

        int count;
        try {
            count = Integer.parseInt(countText.trim());
            if (count < 1 || count > 30) {
                apiStatusLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px; -fx-font-weight: 700;");
                apiStatusLabel.setText("Entre 1 et 30.");
                return;
            }
        } catch (NumberFormatException e) {
            apiStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: 700;");
            apiStatusLabel.setText("Nombre invalide.");
            return;
        }

        generateApiBtn.setDisable(true);
        apiStatusLabel.setStyle("-fx-text-fill: #fcd34d; -fx-font-size: 12px; -fx-font-weight: 700;");
        apiStatusLabel.setText("⏳ Appel API en cours...");

        new Thread(() -> {
            try {
                PandaScoreService apiService = new PandaScoreService();
                int created = apiService.generateTeamsFromApi(count);
                javafx.application.Platform.runLater(() -> {
                    apiStatusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-font-weight: 700;");
                    apiStatusLabel.setText("✅ " + created + " équipe(s) générée(s) !");
                    generateApiBtn.setDisable(false);
                    apiCountField.clear();
                    loadEquipes();
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    apiStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: 700;");
                    apiStatusLabel.setText("❌ Erreur : " + ex.getMessage());
                    generateApiBtn.setDisable(false);
                });
            }
        }).start();
    }
}
