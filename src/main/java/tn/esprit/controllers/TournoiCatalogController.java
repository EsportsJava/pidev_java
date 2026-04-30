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
import javafx.scene.control.Label;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Jeu;
import tn.esprit.entities.Tournoi;
import tn.esprit.entities.User;
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
    private final Map<Integer, String> jeuNoms = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private final List<Tournoi> allTournois = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cardsContainer.setFillWidth(true);
        cardsContainer.setMaxWidth(Double.MAX_VALUE);
        loadJeuNames();
        loadAllTournois();
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
        card.setOnMouseEntered(e -> card.setStyle(CARD_HOVER));
        card.setOnMouseExited(e -> card.setStyle(CARD_BASE));

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
        Button registerBtn = new Button("S'inscrire");
        registerBtn.setCursor(Cursor.HAND);
        registerBtn.setDisable(!canRegister(t));
        registerBtn.setOpacity(registerBtn.isDisabled() ? 0.45 : 1);
        registerBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #db2777, #9333ea); "
                        + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; "
                        + "-fx-background-radius: 999; -fx-padding: 10 26; -fx-cursor: hand;");
        registerBtn.setOnAction(e -> {
            e.consume();
            openInscriptionPage(t);
        });

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

    /**
     * Pas de compteur d'inscrits en base : affichage 0 / max (place libre indicative).
     */
    private static String participantsRatio(Tournoi t) {
        int max = Math.max(0, t.getMaxParticipants());
        return "0/" + max;
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
        return s.contains("planif") || s.contains("cours") || s.contains("ouvert");
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

    private void openInscriptionPage(Tournoi tournoi) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tournoiInscription.fxml"));
            Parent root = loader.load();
            TournoiInscriptionController controller = loader.getController();
            controller.setTournoi(tournoi, jeuNoms.getOrDefault(tournoi.getJeuId(), "—"));
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("Inscription tournoi");
            stage.show();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Ouverture inscription impossible : " + e.getMessage());
        }
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
            participants += t.getMaxParticipants();
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
