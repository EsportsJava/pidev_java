package tn.esprit.controllers.matchgame;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.MatchGame;
import tn.esprit.entities.Tournoi;
import tn.esprit.services.ServiceMatchGame;
import tn.esprit.services.ServiceTournoi;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class MatchGameDashboardController implements Initializable {

    @FXML
    private TableView<MatchGame> matchTable;
    @FXML
    private TableColumn<MatchGame, java.sql.Timestamp> dateCol;
    @FXML
    private TableColumn<MatchGame, Integer> score1Col;
    @FXML
    private TableColumn<MatchGame, Integer> score2Col;
    @FXML
    private TableColumn<MatchGame, String> statutCol;
    @FXML
    private TableColumn<MatchGame, Integer> equipe1Col;
    @FXML
    private TableColumn<MatchGame, Integer> equipe2Col;
    @FXML
    private TableColumn<MatchGame, Integer> tournoiCol;
    @FXML
    private TableColumn<MatchGame, String> actionsCol;
    @FXML
    private Label messageLabel;
    @FXML
    private Label totalMatchsLabel;
    @FXML
    private Label plannedLabel;
    @FXML
    private Label completedLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;

    private final ServiceMatchGame serviceMatchGame = new ServiceMatchGame();
    private final List<MatchGame> allMatchs = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupColumns();
        setupSearchAndSort();
        loadMatchs();
    }

    private void setupSearchAndSort() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Date recente",
                "Date ancienne",
                "ID croissant",
                "ID decroissant",
                "Statut A-Z",
                "Statut Z-A"
        ));
        sortCombo.getSelectionModel().select("Date recente");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSort());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSort());
    }

    private void setupColumns() {
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateMatch"));
        score1Col.setCellValueFactory(new PropertyValueFactory<>("scoreTeam1"));
        score2Col.setCellValueFactory(new PropertyValueFactory<>("scoreTeam2"));
        statutCol.setCellValueFactory(new PropertyValueFactory<>("statut"));
        equipe1Col.setCellValueFactory(new PropertyValueFactory<>("equipe1Id"));
        equipe2Col.setCellValueFactory(new PropertyValueFactory<>("equipe2Id"));
        tournoiCol.setCellValueFactory(new PropertyValueFactory<>("tournoiId"));

        // Style table programmatically (same as Equipe)
        matchTable.setStyle("-fx-background-color: #111b3e; -fx-background-radius: 10;"
            + "-fx-border-color: rgba(124,58,237,0.15); -fx-border-radius: 10;");
        matchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Row factory for dark themed rows with hover
        matchTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<MatchGame> row = new javafx.scene.control.TableRow<>() {
                @Override
                protected void updateItem(MatchGame item, boolean empty) {
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

        // Styled date column
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(java.sql.Timestamp item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    Label lbl = new Label(item.toString());
                    lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
                    setGraphic(lbl); setText(null);
                }
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Styled score1 column
        score1Col.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    Label badge = new Label(String.valueOf(item));
                    badge.setStyle("-fx-background-color: rgba(124,58,237,0.18); -fx-text-fill: #c4b5fd;"
                        + "-fx-padding: 4 14; -fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: 700;");
                    setGraphic(badge); setText(null);
                }
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Styled score2 column
        score2Col.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    Label badge = new Label(String.valueOf(item));
                    badge.setStyle("-fx-background-color: rgba(124,58,237,0.18); -fx-text-fill: #c4b5fd;"
                        + "-fx-padding: 4 14; -fx-background-radius: 8; -fx-font-size: 12px; -fx-font-weight: 700;");
                    setGraphic(badge); setText(null);
                }
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Styled statut column
        statutCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: 700;");
                    setGraphic(lbl); setText(null);
                }
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Styled equipe1 column
        equipe1Col.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    Label lbl = new Label(String.valueOf(item));
                    lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
                    setGraphic(lbl); setText(null);
                }
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Styled equipe2 column
        equipe2Col.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    Label lbl = new Label(String.valueOf(item));
                    lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
                    setGraphic(lbl); setText(null);
                }
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Styled tournoi column
        tournoiCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    Label lbl = new Label(String.valueOf(item));
                    lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
                    setGraphic(lbl); setText(null);
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
                    MatchGame match = getTableView().getItems().get(getIndex());
                    openEditWindow(match);
                });
                deleteBtn.setOnAction(e -> {
                    MatchGame match = getTableView().getItems().get(getIndex());
                    confirmDelete(match);
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

    private void loadMatchs() {
        try {
            List<MatchGame> list = serviceMatchGame.getAll();
            allMatchs.clear();
            allMatchs.addAll(list);
            applyFilterAndSort();
            totalMatchsLabel.setText(String.valueOf(list.size()));
            updateSummaryCounts(list);
            messageLabel.setText("");
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Erreur chargement matchs : " + e.getMessage());
        }
    }

    private void applyFilterAndSort() {
        String keyword = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        List<MatchGame> filtered = new ArrayList<>();
        for (MatchGame match : allMatchs) {
            if (keyword.isEmpty() || matchesKeyword(match, keyword)) {
                filtered.add(match);
            }
        }

        Comparator<MatchGame> comparator = getSortComparator(sortCombo == null ? null : sortCombo.getValue());
        filtered.sort(comparator);
        matchTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private boolean matchesKeyword(MatchGame match, String keyword) {
        return String.valueOf(match.getId()).contains(keyword)
                || String.valueOf(match.getEquipe1Id()).contains(keyword)
                || String.valueOf(match.getEquipe2Id()).contains(keyword)
                || String.valueOf(match.getTournoiId()).contains(keyword)
                || safeLower(match.getStatut()).contains(keyword)
                || safeLower(String.valueOf(match.getDateMatch())).contains(keyword)
                || safeLower(String.valueOf(match.getScoreTeam1())).contains(keyword)
                || safeLower(String.valueOf(match.getScoreTeam2())).contains(keyword);
    }

    private Comparator<MatchGame> getSortComparator(String sortValue) {
        if ("Date ancienne".equals(sortValue)) {
            return Comparator.comparing(MatchGame::getDateMatch, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("ID croissant".equals(sortValue)) {
            return Comparator.comparingInt(MatchGame::getId);
        }
        if ("ID decroissant".equals(sortValue)) {
            return Comparator.comparingInt(MatchGame::getId).reversed();
        }
        if ("Statut A-Z".equals(sortValue)) {
            return Comparator.comparing(m -> safeLower(m.getStatut()));
        }
        if ("Statut Z-A".equals(sortValue)) {
            return Comparator.comparing((MatchGame m) -> safeLower(m.getStatut())).reversed();
        }
        return Comparator.comparing(MatchGame::getDateMatch, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private void updateSummaryCounts(List<MatchGame> matches) {
        int planned = 0;
        int completed = 0;
        for (MatchGame match : matches) {
            String statut = safeLower(match.getStatut());
            if (statut.contains("plan")) {
                planned++;
            }
            if (statut.contains("term") || statut.contains("finish") || statut.contains("fini")) {
                completed++;
            }
        }
        if (plannedLabel != null) {
            plannedLabel.setText(String.valueOf(planned));
        }
        if (completedLabel != null) {
            completedLabel.setText(String.valueOf(completed));
        }
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    @FXML
    private void handleNewMatch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/matchgame/ajouterMatchGame.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter match");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadMatchs();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    @FXML
    private void handleSeedTestMatches() {
        try {
            int inserted = serviceMatchGame.seedSampleMatchGames();
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setText(inserted + " matchs de test ajoutes.");
            loadMatchs();
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Impossible de semer les matchs : " + e.getMessage());
        }
    }

    private void openEditWindow(MatchGame match) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/matchgame/ajouterMatchGame.fxml"));
            Parent root = loader.load();
            AjouterMatchGameController controller = loader.getController();
            controller.setMatchToEdit(match);
            Stage stage = new Stage();
            stage.setTitle("Modifier match");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadMatchs();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    private void confirmDelete(MatchGame match) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer le match #" + match.getId() + " ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    serviceMatchGame.supprimer(match.getId());
                    messageLabel.setStyle("-fx-text-fill: #27ae60;");
                    messageLabel.setText("Match supprime.");
                    loadMatchs();
                } catch (SQLException e) {
                    messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                    messageLabel.setText("Suppression impossible : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleGenerateRoundRobin() {
        handleRoundRobin(false);
    }

    @FXML
    private void handleRegenerateRoundRobin() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation regeneration");
        confirm.setHeaderText(null);
        confirm.setContentText("Regenerer supprimera les matchs existants du tournoi cible. Continuer ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                handleRoundRobin(true);
            }
        });
    }

    private void handleRoundRobin(boolean regenerate) {
        // Load all tournois for the dropdown
        List<Tournoi> tournois;
        try {
            tournois = new ServiceTournoi().getAll();
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Erreur chargement tournois : " + e.getMessage());
            return;
        }

        if (tournois.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Aucun tournoi disponible.");
            return;
        }

        // Build display names list
        List<String> tournoiNames = new ArrayList<>();
        for (Tournoi t : tournois) {
            tournoiNames.add(t.getNom());
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(tournoiNames.get(0), tournoiNames);
        dialog.setTitle(regenerate ? "Regenerer Round-Robin" : "Generer Round-Robin");
        dialog.setHeaderText("Choisir le tournoi");
        dialog.setContentText("Tournoi :");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        // Find the selected tournoi ID
        String selectedName = result.get();
        int tournoiId = -1;
        for (Tournoi t : tournois) {
            if (t.getNom().equals(selectedName)) {
                tournoiId = t.getId();
                break;
            }
        }

        if (tournoiId < 0) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Tournoi introuvable.");
            return;
        }

        try {
            List<Integer> registered = serviceMatchGame.getRegisteredEquipeIds(tournoiId);
            if (registered.size() < 2) {
                Alert ask = new Alert(Alert.AlertType.CONFIRMATION);
                ask.setTitle("Inscription automatique");
                ask.setHeaderText(null);
                ask.setContentText("Moins de 2 equipes inscrites. Inscrire toutes les equipes existantes a ce tournoi ?");
                Optional<ButtonType> answer = ask.showAndWait();
                if (answer.isPresent() && answer.get() == ButtonType.OK) {
                    int inserted = serviceMatchGame.registerAllEquipesToTournoi(tournoiId);
                    messageLabel.setStyle("-fx-text-fill: #22c55e;");
                    messageLabel.setText(inserted + " equipes inscrites automatiquement.");
                }
            }

            int created = serviceMatchGame.generateRoundRobinMatches(tournoiId, regenerate);
            messageLabel.setStyle("-fx-text-fill: #27ae60;");
            messageLabel.setText(created + " matchs round-robin crees pour " + selectedName + ".");
            loadMatchs();
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            messageLabel.setText("Generation impossible : " + e.getMessage());
        }
    }
}
