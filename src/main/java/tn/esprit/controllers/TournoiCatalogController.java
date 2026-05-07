package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Jeu;
import tn.esprit.entities.Tournoi;
import tn.esprit.entities.User;
import tn.esprit.services.NotificationService;
import tn.esprit.services.ServiceJeu;
import tn.esprit.services.ServiceTournoi;
import tn.esprit.services.ServiceTournoiInscription;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * Catalogue public des tournois : cartes pleine largeur (style maquette) + filtres.
 */
public class TournoiCatalogController implements Initializable {

    private static final String CARD_BASE =
            "-fx-background-color: #1a0b2e; -fx-background-radius: 14; -fx-border-radius: 14; "
                    + "-fx-border-color: #d63384; -fx-border-width: 2;";
    private static final String CARD_HOVER =
            "-fx-background-color: #221238; -fx-background-radius: 14; -fx-border-radius: 14; "
                    + "-fx-border-color: #f472b6; -fx-border-width: 2;";

    private static final DateTimeFormatter CARD_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    @FXML
    private VBox cardsContainer;
    @FXML
    private Label messageLabel;
    @FXML
    private Label totalTournoisLabel;
    @FXML
    private Label ouvertsLabel;
    @FXML
    private Label totalCagnotteLabel;
    @FXML
    private Label totalParticipantsLabel;
    @FXML
    private Button allFilterBtn;
    @FXML
    private Button openFilterBtn;
    @FXML
    private Button closedFilterBtn;
    @FXML
    private Button soloFilterBtn;
    @FXML
    private Button teamFilterBtn;
    @FXML
    private Button recommendedFilterBtn;

    private final ServiceTournoi serviceTournoi = new ServiceTournoi();
    private final ServiceJeu serviceJeu = new ServiceJeu();
    private final ServiceTournoiInscription serviceInscription = new ServiceTournoiInscription();
    private final NotificationService notificationService = new NotificationService();
    private final Map<Integer, String> jeuNoms = new HashMap<>();
    private final Map<Integer, Integer> participantsByTournoi = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private final List<Tournoi> allTournois = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cardsContainer.setFillWidth(true);
        cardsContainer.setMaxWidth(Double.MAX_VALUE);
        loadJeuNames();
        loadAllTournois();
        loadParticipantsCounts();
        updateHeaderStats();
        showTournois(allTournois);
        setActiveFilterButton(allFilterBtn);
    }

    private void loadJeuNames() {
        try {
            List<Jeu> jeux = serviceJeu.getAll();
            for (Jeu j : jeux) {
                jeuNoms.put(j.getId(), j.getNom());
            }
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Impossible de charger les jeux : " + e.getMessage());
        }
    }

    private void loadAllTournois() {
        allTournois.clear();
        try {
            allTournois.addAll(serviceTournoi.getAll());
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Erreur lors du chargement des tournois : " + e.getMessage());
        }
    }

    private void loadParticipantsCounts() {
        participantsByTournoi.clear();
        try {
            serviceInscription.ensureTable();
            for (Tournoi t : allTournois) {
                participantsByTournoi.put(t.getId(), serviceInscription.countByTournoi(t.getId()));
            }
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #fbbf24;");
            messageLabel.setText("Compteur participants indisponible : " + e.getMessage());
        }
    }

    private void showTournois(List<Tournoi> tournois) {
        cardsContainer.getChildren().clear();
        if (tournois.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: #cbd5e1;");
            messageLabel.setText("Aucun tournoi pour ce filtre.");
            return;
        }
        messageLabel.setText("");
        for (Tournoi t : tournois) {
            HBox card = createCard(t);
            card.setMaxWidth(Double.MAX_VALUE);
            cardsContainer.getChildren().add(card);
        }
    }

    private HBox createCard(Tournoi t) {
        HBox card = new HBox(20);
        card.setPadding(new Insets(16, 22, 18, 22));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(CARD_BASE);
        card.setCursor(Cursor.HAND);
        card.setOnMouseEntered(e -> card.setStyle(CARD_HOVER));
        card.setOnMouseExited(e -> card.setStyle(CARD_BASE));
        card.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return; // don't navigate if clicking a button
            openMatchsPage(t);
        });

        Label title = new Label(t.getNom() == null ? "Tournoi" : t.getNom());
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 17px; -fx-font-weight: 600;");
        title.setWrapText(true);
        title.setMaxWidth(460);

        String jeuNom = jeuNoms.getOrDefault(t.getJeuId(), "—");
        Label jeu = new Label(jeuNom);
        jeu.setStyle("-fx-text-fill: #f472b6; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label dateRow = new Label("📅  " + formatDateRange(t));
        dateRow.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        dateRow.setWrapText(true);

        String tzShort = ZoneId.systemDefault().getDisplayName(TextStyle.SHORT_STANDALONE, Locale.FRENCH);
        Label tz = new Label(tzShort);
        tz.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        VBox left = new VBox(8, title, jeu, dateRow, tz);
        left.setMinWidth(220);
        left.setPrefWidth(320);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        VBox partBox = statBox("Participants", participantsRatio(t));
        VBox cagBox = statBox("Cagnotte", formatCagnotteCard(t.getCagnotte()));
        HBox statsRow = new HBox(14, partBox, cagBox);
        statsRow.setAlignment(Pos.CENTER);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Node statusPill = createStatusPill(t);

        // Check if user already has a team inscribed
        User currentUser = SessionManager.getCurrentUser();
        Equipe alreadyInscribed = null;
        if (currentUser != null && currentUser.getId() > 0) {
            try {
                alreadyInscribed = serviceInscription.getInscribedEquipeForUser(t.getId(), currentUser.getId());
            } catch (SQLException ignored) {}
        }

        Button registerBtn;
        if (alreadyInscribed != null) {
            // Team already registered — show "Quitter" button
            final Equipe inscribedEquipe = alreadyInscribed;
            registerBtn = new Button("Quitter");
            registerBtn.setCursor(Cursor.HAND);
            registerBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #dc2626, #b91c1c); "
                            + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; "
                            + "-fx-background-radius: 999; -fx-padding: 10 26; -fx-cursor: hand;");
            registerBtn.setOnAction(e -> {
                e.consume();
                handleQuitterTournoi(t, currentUser, inscribedEquipe);
            });
        } else {
            // Not registered — show "S'inscrire" button
            registerBtn = new Button("S'inscrire");
            registerBtn.setCursor(Cursor.HAND);
            registerBtn.setDisable(!canRegister(t));
            registerBtn.setOpacity(registerBtn.isDisabled() ? 0.45 : 1);
            registerBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #db2777, #9333ea); "
                            + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; "
                            + "-fx-background-radius: 999; -fx-padding: 10 26; -fx-cursor: hand;");
            registerBtn.setOnAction(e -> {
                e.consume();
                showInscriptionPopup(t);
            });
        }

        VBox right = new VBox(12, statusPill, registerBtn);
        right.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(left, spacer1, statsRow, spacer2, right);
        return card;
    }

    private static VBox statBox(String title, String value) {
        Label l = new Label(title);
        l.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #ec4899; -fx-font-size: 24px; -fx-font-weight: bold;");
        VBox box = new VBox(8, l, v);
        box.setAlignment(Pos.CENTER);
        box.setMinWidth(118);
        box.setPadding(new Insets(14, 20, 14, 20));
        box.setStyle("-fx-background-color: rgba(0,0,0,0.38); -fx-background-radius: 12;");
        return box;
    }

    private String participantsRatio(Tournoi t) {
        int used = participantsByTournoi.getOrDefault(t.getId(), 0);
        int max = Math.max(0, t.getMaxParticipants());
        return used + "/" + max;
    }

    private static String formatCagnotteCard(double amount) {
        if (Math.floor(amount) == amount) {
            return ((long) amount) + " TND";
        }
        return String.format(Locale.FRANCE, "%.0f TND", amount);
    }

    private String formatDateRange(Tournoi t) {
        LocalDate d1 = toLocalDate(t.getDateDebut());
        LocalDate d2 = toLocalDate(t.getDateFin());
        if (d1 == null && d2 == null) {
            return "—";
        }
        if (d1 == null) {
            return CARD_DATE.format(d2);
        }
        if (d2 == null) {
            return CARD_DATE.format(d1);
        }
        return CARD_DATE.format(d1) + " - " + CARD_DATE.format(d2);
    }

    private static LocalDate toLocalDate(java.util.Date d) {
        if (d == null) {
            return null;
        }
        if (d instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Node createStatusPill(Tournoi t) {
        String stat = t.getStatut() == null ? "" : t.getStatut().trim();
        String s = stat.toLowerCase(Locale.ROOT);
        String text;
        String bg;
        String fg;
        if (s.contains("annul")) {
            text = "●  Annulé";
            bg = "#fee2e2";
            fg = "#991b1b";
        } else if (s.contains("ferm")) {
            text = "●  Fermé";
            bg = "#e2e8f0";
            fg = "#334155";
        } else if (s.contains("termin")) {
            text = "●  Terminé";
            bg = "#e2e8f0";
            fg = "#334155";
        } else if (s.contains("cours")) {
            text = "●  En cours";
            bg = "#d1fae5";
            fg = "#065f46";
        } else {
            text = "⏱  En attente";
            bg = "#fef3c7";
            fg = "#78350f";
        }
        Label pill = new Label(text);
        pill.setStyle(String.format(
                Locale.ROOT,
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-font-weight: bold; "
                        + "-fx-padding: 8 16; -fx-background-radius: 999;",
                bg, fg));
        return pill;
    }

    private boolean canRegister(Tournoi t) {
        if (t.getStatut() == null) {
            return false;
        }
        String s = t.getStatut().trim().toLowerCase(Locale.ROOT);
        if (s.contains("annul") || s.contains("termin") || s.contains("ferm")) {
            return false;
        }
        int used = participantsByTournoi.getOrDefault(t.getId(), 0);
        int max = Math.max(0, t.getMaxParticipants());
        boolean hasSlots = max <= 0 || used < max;
        return hasSlots && (s.contains("planif") || s.contains("cours") || s.contains("ouvert"));
    }

    private void showTournoiDetails(Tournoi tournoi) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Détails du tournoi");
        info.setHeaderText(tournoi.getNom() == null ? "Tournoi" : tournoi.getNom());
        info.setContentText(
                "Jeu: " + jeuNoms.getOrDefault(tournoi.getJeuId(), "N/A") + "\n"
                        + "Type: " + normalizeTypeLabel(tournoi.getType()) + "\n"
                        + "Statut: " + (tournoi.getStatut() != null ? tournoi.getStatut() : "—") + "\n"
                        + "Date début: " + formatDate(tournoi.getDateDebut()) + "\n"
                        + "Date fin: " + formatDate(tournoi.getDateFin()) + "\n"
                        + "Limite inscription: " + formatDate(tournoi.getDateInscriptionLimite()) + "\n"
                        + "Participants (places): " + participantsRatio(tournoi) + "\n"
                        + "Frais inscription: " + formatMoney(tournoi.getFraisInscription()) + "\n"
                        + "Cagnotte: " + formatMoney(tournoi.getCagnotte())
        );
        info.showAndWait();
    }

    private void openMatchsPage(Tournoi tournoi) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tournoiMatchs.fxml"));
            Parent root = loader.load();
            TournoiMatchsController controller = loader.getController();
            controller.setTournoi(tournoi, jeuNoms.getOrDefault(tournoi.getJeuId(), "—"));
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("Matchs — " + (tournoi.getNom() != null ? tournoi.getNom() : "Tournoi"));
            stage.show();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Ouverture impossible : " + e.getMessage());
        }
    }

    private void showInscriptionPopup(Tournoi tournoi) {
        User user = SessionManager.getCurrentUser();
        if (user == null || user.getId() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Connexion requise", "Connectez-vous pour inscrire une équipe.");
            return;
        }

        List<Equipe> ownedTeams;
        try {
            ownedTeams = serviceInscription.getOwnedEquipesForUser(user.getId());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger vos équipes : " + e.getMessage());
            return;
        }

        if (ownedTeams.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Aucune équipe",
                    "Vous ne possédez aucune équipe.\nCréez une équipe d'abord pour pouvoir l'inscrire.");
            return;
        }

        // ── Custom dark-themed popup ──
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initOwner(cardsContainer.getScene().getWindow());

        // ── Header with gradient ──
        Label icon = new Label("🏆");
        icon.setStyle("-fx-font-size: 28px;");
        StackPane iconCircle = new StackPane(icon);
        iconCircle.setMinSize(56, 56);
        iconCircle.setMaxSize(56, 56);
        iconCircle.setStyle("-fx-background-color: linear-gradient(to bottom right, #7c3aed, #db2777); -fx-background-radius: 50;");

        Label titleLabel = new Label("Inscription au tournoi");
        titleLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 20px; -fx-font-weight: 800;");

        String tName = tournoi.getNom() == null ? "Tournoi" : tournoi.getNom();
        Label subtitleLabel = new Label(tName);
        subtitleLabel.setStyle("-fx-text-fill: #f472b6; -fx-font-size: 14px; -fx-font-weight: 600;");

        // Places info
        int used = 0;
        try { used = serviceInscription.countByTournoi(tournoi.getId()); } catch (SQLException ignored) {}
        int max = Math.max(0, tournoi.getMaxParticipants());
        Label placesLabel = new Label("📊  Places : " + used + " / " + max);
        placesLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        VBox headerInfo = new VBox(4, titleLabel, subtitleLabel, placesLabel);
        headerInfo.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(16, iconCircle, headerInfo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24, 28, 16, 28));
        header.setStyle("-fx-background-color: linear-gradient(to right, #0d1b3e, #1a0b2e); -fx-background-radius: 16 16 0 0;");

        // ── Separator ──
        Region separator = new Region();
        separator.setMinHeight(2);
        separator.setMaxHeight(2);
        separator.setStyle("-fx-background-color: linear-gradient(to right, #7c3aed, #db2777, #7c3aed);");

        // ── Body ──
        Label selectLabel = new Label("Choisir l'équipe à inscrire");
        selectLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");

        ComboBox<Equipe> combo = new ComboBox<>();
        combo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Equipe item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8;");
                } else {
                    setText("👥  " + item.getNom());
                    setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-padding: 8 12;");
                }
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Equipe item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("👥  " + item.getNom());
                }
                setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-background-color: #1e293b;");
            }
        });
        combo.getItems().addAll(ownedTeams);
        combo.getSelectionModel().selectFirst();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 10; -fx-font-size: 13px; -fx-pref-height: 42; -fx-mark-color: #e2e8f0;");

        Label statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 12px;");

        VBox body = new VBox(12, selectLabel, combo, statusLabel);
        body.setPadding(new Insets(20, 28, 10, 28));

        // ── Buttons ──
        Button confirmBtn = new Button("✅  Inscrire l'équipe");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);
        confirmBtn.setCursor(Cursor.HAND);
        String confirmStyle = "-fx-background-color: linear-gradient(to right, #db2777, #9333ea); "
                + "-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 14px; "
                + "-fx-background-radius: 12; -fx-pref-height: 46; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(219,39,119,0.45), 12, 0, 0, 4);";
        String confirmHover = "-fx-background-color: linear-gradient(to right, #ec4899, #a855f7); "
                + "-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 14px; "
                + "-fx-background-radius: 12; -fx-pref-height: 46; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(236,72,153,0.65), 18, 0, 0, 5);";
        confirmBtn.setStyle(confirmStyle);
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(confirmHover));
        confirmBtn.setOnMouseExited(e -> confirmBtn.setStyle(confirmStyle));

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setCursor(Cursor.HAND);
        String cancelStyle = "-fx-background-color: transparent; -fx-text-fill: #94a3b8; "
                + "-fx-font-size: 13px; -fx-background-radius: 10; -fx-pref-height: 38; "
                + "-fx-border-color: #334155; -fx-border-radius: 10; -fx-cursor: hand;";
        String cancelHover = "-fx-background-color: rgba(51,65,85,0.3); -fx-text-fill: #e2e8f0; "
                + "-fx-font-size: 13px; -fx-background-radius: 10; -fx-pref-height: 38; "
                + "-fx-border-color: #475569; -fx-border-radius: 10; -fx-cursor: hand;";
        cancelBtn.setStyle(cancelStyle);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelHover));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(cancelStyle));
        cancelBtn.setOnAction(e -> popup.close());

        VBox buttons = new VBox(10, confirmBtn, cancelBtn);
        buttons.setPadding(new Insets(6, 28, 24, 28));

        // ── Assemble ──
        VBox root = new VBox(0, header, separator, body, buttons);
        root.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 16; "
                + "-fx-border-color: rgba(124,58,237,0.3); -fx-border-radius: 16; -fx-border-width: 1.5;");
        root.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.7)));
        root.setPrefWidth(440);

        StackPane wrapper = new StackPane(root);
        wrapper.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        wrapper.setPadding(new Insets(40));

        confirmBtn.setOnAction(e -> {
            Equipe selected = combo.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                statusLabel.setText("Veuillez sélectionner une équipe.");
                return;
            }
            popup.close();
            doInscrireEquipe(tournoi, user, selected);
        });

        Scene scene = new Scene(wrapper);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.showAndWait();
    }

    private void doInscrireEquipe(Tournoi tournoi, User user, Equipe equipe) {
        try {
            serviceInscription.ensureTable();
            int used = serviceInscription.countByTournoi(tournoi.getId());
            int max = Math.max(0, tournoi.getMaxParticipants());
            if (max > 0 && used >= max) {
                showAlert(Alert.AlertType.WARNING, "Tournoi complet",
                        "Plus de places disponibles (" + used + "/" + max + ").");
                return;
            }
            if (serviceInscription.existsForEquipe(tournoi.getId(), equipe.getId())) {
                showAlert(Alert.AlertType.WARNING, "Déjà inscrite",
                        "L'équipe \"" + equipe.getNom() + "\" est déjà inscrite à ce tournoi.");
                return;
            }
            serviceInscription.inscrire(tournoi.getId(), user.getId(), equipe.getId(),
                    user.getNom(), user.getEmail());
            notificationService.createNotificationIfAbsent(
                    user.getId(),
                    "inscription_confirmed",
                    "Inscription confirmée",
                    "L'équipe \"" + equipe.getNom() + "\" est inscrite au tournoi \""
                            + (tournoi.getNom() == null ? "Tournoi" : tournoi.getNom()) + "\".",
                    "inscription:" + tournoi.getId() + ":equipe:" + equipe.getId());

            // Refresh UI
            loadParticipantsCounts();
            updateHeaderStats();
            showTournois(allTournois);

            showAlert(Alert.AlertType.INFORMATION, "Inscription confirmée",
                    "L'équipe \"" + equipe.getNom() + "\" a été inscrite avec succès !");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur d'inscription",
                    "Impossible d'inscrire l'équipe : " + e.getMessage());
        }
    }

    private void handleQuitterTournoi(Tournoi tournoi, User user, Equipe equipe) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Quitter le tournoi");
        confirm.setHeaderText(null);
        confirm.setContentText("Retirer l'équipe \"" + equipe.getNom() + "\" du tournoi \""
                + (tournoi.getNom() == null ? "Tournoi" : tournoi.getNom()) + "\" ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    serviceInscription.desinscrireEquipe(tournoi.getId(), equipe.getId());

                    // Refresh UI
                    loadParticipantsCounts();
                    updateHeaderStats();
                    showTournois(allTournois);

                    showAlert(Alert.AlertType.INFORMATION, "Désinscription confirmée",
                            "L'équipe \"" + equipe.getNom() + "\" a été retirée du tournoi.");
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible de quitter le tournoi : " + e.getMessage());
                }
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void updateHeaderStats() {
        int total = allTournois.size();
        int ouverts = 0;
        double cagnotte = 0;
        int participants = 0;

        for (Tournoi t : allTournois) {
            if (isOpenStatus(t.getStatut())) {
                ouverts++;
            }
            cagnotte += t.getCagnotte();
            participants += participantsByTournoi.getOrDefault(t.getId(), 0);
        }

        totalTournoisLabel.setText(String.valueOf(total));
        ouvertsLabel.setText(String.valueOf(ouverts));
        totalCagnotteLabel.setText(formatMoney(cagnotte));
        totalParticipantsLabel.setText(String.valueOf(participants));
    }

    @FXML
    private void filterAll() {
        setActiveFilterButton(allFilterBtn);
        showTournois(allTournois);
    }

    @FXML
    private void filterOpen() {
        setActiveFilterButton(openFilterBtn);
        showTournois(filterBy(this::isTournoiOpen));
    }

    @FXML
    private void filterClosed() {
        setActiveFilterButton(closedFilterBtn);
        showTournois(filterBy(t -> !isTournoiOpen(t)));
    }

    @FXML
    private void filterSolo() {
        setActiveFilterButton(soloFilterBtn);
        showTournois(filterBy(t -> normalizeTypeLabel(t.getType()).equalsIgnoreCase("solo")));
    }

    @FXML
    private void filterTeam() {
        setActiveFilterButton(teamFilterBtn);
        showTournois(filterBy(t -> normalizeTypeLabel(t.getType()).equalsIgnoreCase("team")));
    }

    @FXML
    private void filterRecommended() {
        setActiveFilterButton(recommendedFilterBtn);
        User user = SessionManager.getCurrentUser();
        if (user == null || user.getId() <= 0) {
            messageLabel.setStyle("-fx-text-fill: #fbbf24;");
            messageLabel.setText("Connectez-vous pour recevoir des recommandations personnalisées.");
            showTournois(filterBy(this::isTournoiOpen));
            return;
        }
        try {
            List<Tournoi> recs = buildRecommendations(user.getId());
            if (recs.isEmpty()) {
                messageLabel.setStyle("-fx-text-fill: #cbd5e1;");
                messageLabel.setText("Aucune recommandation disponible pour le moment.");
            } else {
                messageLabel.setStyle("-fx-text-fill: #93c5fd;");
                messageLabel.setText("Recommandations basées sur votre historique, niveau et créneau.");
            }
            showTournois(recs);
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Recommandation indisponible : " + e.getMessage());
            showTournois(filterBy(this::isTournoiOpen));
        }
    }

    private List<Tournoi> filterBy(Predicate<Tournoi> predicate) {
        List<Tournoi> filtered = new ArrayList<>();
        for (Tournoi t : allTournois) {
            if (predicate.test(t)) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    @FXML
    private void openKpiPage() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tournoiKpi.fxml"));
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("KPIs Tournois");
            stage.show();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Ouverture KPIs impossible : " + e.getMessage());
        }
    }

    private void setActiveFilterButton(Button activeButton) {
        String idle = "-fx-background-color: #26365f; -fx-text-fill: #e2e8f0; -fx-background-radius: 999; "
                + "-fx-padding: 6 16; -fx-cursor: hand;";
        String active = "-fx-background-color: linear-gradient(to right, #be185d, #7c3aed); -fx-text-fill: #ffffff; "
                + "-fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 6 16; -fx-cursor: hand;";
        allFilterBtn.setStyle(idle);
        openFilterBtn.setStyle(idle);
        closedFilterBtn.setStyle(idle);
        soloFilterBtn.setStyle(idle);
        teamFilterBtn.setStyle(idle);
        if (recommendedFilterBtn != null) {
            recommendedFilterBtn.setStyle(idle);
        }
        activeButton.setStyle(active);
    }

    private boolean isTournoiOpen(Tournoi tournoi) {
        return isOpenStatus(tournoi.getStatut());
    }

    private boolean isOpenStatus(String statut) {
        if (statut == null) {
            return false;
        }
        String s = statut.trim().toLowerCase(Locale.ROOT);
        return s.contains("ouvert") || s.contains("cours") || s.contains("planifi");
    }

    private String normalizeTypeLabel(String type) {
        if (type == null || type.isBlank()) {
            return "N/A";
        }
        String t = type.trim().toLowerCase(Locale.ROOT);
        if (t.contains("solo")) {
            return "solo";
        }
        if (t.contains("team") || t.contains("équipe") || t.contains("equipe")) {
            return "team";
        }
        return type.trim();
    }

    private String formatMoney(double amount) {
        if (Math.floor(amount) == amount) {
            return ((long) amount) + " TND";
        }
        return String.format(Locale.FRANCE, "%.2f TND", amount);
    }

    private String formatDate(java.util.Date date) {
        return date == null ? "-" : dateFormat.format(date);
    }

    private List<Tournoi> buildRecommendations(int userId) throws SQLException {
        Map<Integer, Integer> jeuCount = serviceInscription.countByUserByJeu(userId);
        DayOfWeek preferredDay = serviceInscription.mostFrequentDayByUser(userId);
        int totalPast = 0;
        int bestJeuId = -1;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> e : jeuCount.entrySet()) {
            totalPast += e.getValue();
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                bestJeuId = e.getKey();
            }
        }

        LocalDate today = LocalDate.now();
        List<TournoiScore> scored = new ArrayList<>();
        for (Tournoi t : allTournois) {
            if (!isTournoiOpen(t)) {
                continue;
            }
            int score = 0;

            // Jeu favori: gros poids
            if (bestJeuId > 0 && t.getJeuId() == bestJeuId) {
                score += 50;
            } else if (jeuCount.containsKey(t.getJeuId())) {
                score += 30;
            }

            // Niveau estimé via historique participations
            int levelBoost;
            if (totalPast >= 12) {
                levelBoost = 20; // avancé
            } else if (totalPast >= 5) {
                levelBoost = 12; // intermédiaire
            } else {
                levelBoost = 6;  // débutant
            }
            score += levelBoost;

            // Créneau horaire/jour préféré
            LocalDate start = toLocalDate(t.getDateDebut());
            if (start != null && preferredDay != null && start.getDayOfWeek() == preferredDay) {
                score += 18;
            }

            // Tournois proches dans le temps
            if (start != null) {
                long gap = Math.abs(today.toEpochDay() - start.toEpochDay());
                if (gap <= 3) {
                    score += 20;
                } else if (gap <= 7) {
                    score += 12;
                } else if (gap <= 14) {
                    score += 6;
                }
            }

            scored.add(new TournoiScore(t, score));
        }

        scored.sort(Comparator.comparingInt(TournoiScore::score).reversed());
        List<Tournoi> out = new ArrayList<>();
        for (TournoiScore ts : scored) {
            out.add(ts.tournoi());
        }
        return out;
    }

    private record TournoiScore(Tournoi tournoi, int score) {
    }
}
