package tn.esprit.controllers;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert;
import javafx.scene.Cursor;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Jeu;
import tn.esprit.entities.Tournoi;
import tn.esprit.services.ServiceJeu;
import tn.esprit.services.ServiceTournoi;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class TournoiCatalogController implements Initializable {
    private static final String CARD_STYLE =
            "-fx-background-color: #1e2d4a; -fx-background-radius: 10; -fx-border-radius: 10; "
                    + "-fx-border-color: rgba(167,139,250,0.35); -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 12, 0.15, 0, 4);";
    private static final String CARD_STYLE_HOVER =
            "-fx-background-color: #26365f; -fx-background-radius: 10; -fx-border-radius: 10; "
                    + "-fx-border-color: rgba(167,139,250,0.80); -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.48), 16, 0.2, 0, 6);";

    @FXML
    private FlowPane cardsContainer;
    @FXML
    private ScrollPane cardsScrollPane;
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

    private final ServiceTournoi serviceTournoi = new ServiceTournoi();
    private final ServiceJeu serviceJeu = new ServiceJeu();
    private final Map<Integer, String> jeuNoms = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private final List<Tournoi> allTournois = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cardsContainer.setAlignment(Pos.TOP_CENTER);
        cardsContainer.prefWrapLengthProperty().bind(Bindings.min(cardsScrollPane.widthProperty().subtract(40), 1082));
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
            cardsContainer.getChildren().add(createCard(t));
        }
    }

    private VBox createCard(Tournoi t) {
        Label nomLabel = new Label(t.getNom() == null ? "Tournoi" : t.getNom());
        nomLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 16px; -fx-font-weight: bold;");
        nomLabel.setMaxWidth(230);
        nomLabel.setWrapText(true);
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label statutBadge = createBadge(normalizeStatusLabel(t.getStatut()), badgeStyleForStatus(t.getStatut()));
        RegionSpacer spacer = new RegionSpacer();
        topRow.getChildren().addAll(nomLabel, spacer, statutBadge);

        Label jeuLabel = new Label("Jeu: " + jeuNoms.getOrDefault(t.getJeuId(), "N/A"));
        jeuLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label typeBadge = createBadge(normalizeTypeLabel(t.getType()), "-fx-background-color: #334155; -fx-text-fill: #e2e8f0;");
        HBox tagsRow = new HBox(6, typeBadge);
        tagsRow.setAlignment(Pos.CENTER_LEFT);
        tagsRow.setPadding(new Insets(0, 0, 2, 0));

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: rgba(255,255,255,0.10);");

        HBox dateRow = kvRow("Date", formatDate(t.getDateDebut()));
        HBox participantsRow = kvRow("Max participants", String.valueOf(t.getMaxParticipants()));
        HBox fraisRow = kvRow("Frais inscription", formatMoney(t.getFraisInscription()));

        Label cagnotteTitle = new Label("Cagnotte");
        cagnotteTitle.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        Label cagnotteValue = new Label(formatMoney(t.getCagnotte()));
        cagnotteValue.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label(truncate((t.getDescription() == null || t.getDescription().isBlank())
                ? "Pas de description."
                : t.getDescription(), 85));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        VBox card = new VBox(7, topRow, jeuLabel, tagsRow, divider, dateRow, participantsRow, fraisRow, cagnotteTitle, cagnotteValue, descriptionLabel);
        card.setPadding(new Insets(16));
        card.setPrefWidth(350);
        card.setMaxWidth(350);
        card.setMinHeight(238);
        card.setStyle(CARD_STYLE);
        configureCardInteraction(card, t);
        return card;
    }

    private void configureCardInteraction(VBox card, Tournoi tournoi) {
        card.setCursor(Cursor.HAND);
        card.setOnMouseEntered(e -> card.setStyle(CARD_STYLE_HOVER));
        card.setOnMouseExited(e -> card.setStyle(CARD_STYLE));
        card.setOnMouseClicked(e -> showTournoiDetails(tournoi));
    }

    private void showTournoiDetails(Tournoi tournoi) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Détails du tournoi");
        info.setHeaderText(tournoi.getNom() == null ? "Tournoi" : tournoi.getNom());
        info.setContentText(
                "Jeu: " + jeuNoms.getOrDefault(tournoi.getJeuId(), "N/A") + "\n"
                        + "Type: " + normalizeTypeLabel(tournoi.getType()) + "\n"
                        + "Statut: " + normalizeStatusLabel(tournoi.getStatut()) + "\n"
                        + "Date début: " + formatDate(tournoi.getDateDebut()) + "\n"
                        + "Date fin: " + formatDate(tournoi.getDateFin()) + "\n"
                        + "Max participants: " + tournoi.getMaxParticipants() + "\n"
                        + "Cagnotte: " + formatMoney(tournoi.getCagnotte())
        );
        info.showAndWait();
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

    private List<Tournoi> filterBy(Predicate<Tournoi> predicate) {
        List<Tournoi> filtered = new ArrayList<>();
        for (Tournoi t : allTournois) {
            if (predicate.test(t)) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    private void setActiveFilterButton(Button activeButton) {
        String idle = "-fx-background-color: #26365f; -fx-text-fill: #e2e8f0; -fx-background-radius: 8; -fx-cursor: hand;";
        String active = "-fx-background-color: #7c3aed; -fx-text-fill: #ffffff; -fx-border-color: #a78bfa; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        allFilterBtn.setStyle(idle);
        openFilterBtn.setStyle(idle);
        closedFilterBtn.setStyle(idle);
        soloFilterBtn.setStyle(idle);
        teamFilterBtn.setStyle(idle);
        activeButton.setStyle(active);
    }

    private boolean isTournoiOpen(Tournoi tournoi) {
        return isOpenStatus(tournoi.getStatut());
    }

    private boolean isOpenStatus(String statut) {
        if (statut == null) {
            return false;
        }
        String s = statut.trim().toLowerCase();
        return s.contains("ouvert") || s.contains("cours") || s.contains("planifi");
    }

    private String normalizeTypeLabel(String type) {
        if (type == null || type.isBlank()) {
            return "N/A";
        }
        String t = type.trim().toLowerCase();
        if (t.contains("solo")) {
            return "solo";
        }
        if (t.contains("team") || t.contains("équipe") || t.contains("equipe")) {
            return "team";
        }
        return type.trim();
    }

    private String normalizeStatusLabel(String statut) {
        if (statut == null || statut.isBlank()) {
            return "inconnu";
        }
        return isOpenStatus(statut) ? "ouvert" : "fermé";
    }

    private String badgeStyleForStatus(String statut) {
        if (isOpenStatus(statut)) {
            return "-fx-background-color: #d9f99d; -fx-text-fill: #365314; -fx-font-weight: bold;";
        }
        return "-fx-background-color: #fecaca; -fx-text-fill: #7f1d1d; -fx-font-weight: bold;";
    }

    private Label createBadge(String text, String style) {
        Label badge = new Label(text);
        badge.setStyle(style + " -fx-font-size: 10px; -fx-padding: 2 8 2 8; -fx-background-radius: 999;");
        return badge;
    }

    private HBox kvRow(String key, String value) {
        Label left = new Label(key);
        left.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        Label right = new Label(value);
        right.setStyle("-fx-text-fill: #f3f4f6; -fx-font-size: 11px; -fx-font-weight: bold;");
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        RegionSpacer spacer = new RegionSpacer();
        row.getChildren().addAll(left, spacer, right);
        return row;
    }

    private String formatMoney(double amount) {
        if (Math.floor(amount) == amount) {
            return ((long) amount) + " TND";
        }
        return String.format("%.2f TND", amount);
    }

    private String formatDate(java.util.Date date) {
        return date == null ? "-" : dateFormat.format(date);
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 1) + "…";
    }

    private static class RegionSpacer extends javafx.scene.layout.Region {
        RegionSpacer() {
            HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS);
        }
    }
}
