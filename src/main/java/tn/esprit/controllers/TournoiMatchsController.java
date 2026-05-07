package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.MatchGame;
import tn.esprit.entities.Tournoi;
import tn.esprit.services.ServiceEquipe;
import tn.esprit.services.ServiceMatchGame;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class TournoiMatchsController implements Initializable {

    @FXML private VBox mainContent;
    @FXML private VBox matchCardsContainer;
    @FXML private Label tournoiNameLabel;
    @FXML private Label tournoiInfoLabel;
    @FXML private Label tournoiDateLabel;
    @FXML private Label matchCountLabel;
    @FXML private Label finishedCountLabel;
    @FXML private Label messageLabel;

    private final ServiceMatchGame serviceMatchGame = new ServiceMatchGame();
    private final Map<Integer, String> equipeNames = new HashMap<>();
    private Tournoi tournoi;
    private String jeuNom;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH);
    private static final SimpleDateFormat MATCH_DATE_FMT =
            new SimpleDateFormat("dd/MM/yyyy  HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadEquipeNames();
    }

    public void setTournoi(Tournoi tournoi, String jeuNom) {
        this.tournoi = tournoi;
        this.jeuNom = jeuNom;
        populateHeader();
        loadMatchs();
    }

    private void loadEquipeNames() {
        try {
            for (Equipe e : new ServiceEquipe().getAll()) {
                equipeNames.put(e.getId(), e.getNom() != null ? e.getNom() : "Équipe #" + e.getId());
            }
        } catch (SQLException ignored) {}
    }

    private void populateHeader() {
        tournoiNameLabel.setText(tournoi.getNom() == null ? "Tournoi" : tournoi.getNom());
        tournoiInfoLabel.setText("🎮  " + (jeuNom != null ? jeuNom : "—") + "   •   "
                + (tournoi.getType() != null ? tournoi.getType() : "—"));

        String dateStr = "📅  ";
        LocalDate d1 = toLocalDate(tournoi.getDateDebut());
        LocalDate d2 = toLocalDate(tournoi.getDateFin());
        if (d1 != null && d2 != null) {
            dateStr += DATE_FMT.format(d1) + "  →  " + DATE_FMT.format(d2);
        } else if (d1 != null) {
            dateStr += DATE_FMT.format(d1);
        } else {
            dateStr += "—";
        }
        tournoiDateLabel.setText(dateStr);
    }

    private void loadMatchs() {
        matchCardsContainer.getChildren().clear();
        try {
            List<MatchGame> matches = serviceMatchGame.getByTournoi(tournoi.getId());
            matchCountLabel.setText(String.valueOf(matches.size()));

            int finished = 0;
            for (MatchGame m : matches) {
                String s = m.getStatut() == null ? "" : m.getStatut().toLowerCase(Locale.ROOT);
                if (s.contains("term") || s.contains("finish") || s.contains("fini")) {
                    finished++;
                }
            }
            finishedCountLabel.setText(String.valueOf(finished));

            if (matches.isEmpty()) {
                messageLabel.setText("Aucun match pour ce tournoi.");
                return;
            }
            messageLabel.setText("");

            for (MatchGame m : matches) {
                matchCardsContainer.getChildren().add(createMatchCard(m));
            }
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px;");
            messageLabel.setText("Erreur chargement matchs : " + e.getMessage());
        }
    }

    private HBox createMatchCard(MatchGame m) {
        // ── Card container ──
        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(0));
        String cardBase = "-fx-background-color: #111b3e; -fx-background-radius: 14; "
                + "-fx-border-color: rgba(124,58,237,0.2); -fx-border-radius: 14; -fx-border-width: 1;";
        String cardHover = "-fx-background-color: #151f42; -fx-background-radius: 14; "
                + "-fx-border-color: rgba(124,58,237,0.4); -fx-border-radius: 14; -fx-border-width: 1;";
        card.setStyle(cardBase);
        card.setOnMouseEntered(e -> card.setStyle(cardHover));
        card.setOnMouseExited(e -> card.setStyle(cardBase));
        card.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.25)));

        // ── Status indicator (left bar) ──
        String statut = m.getStatut() == null ? "" : m.getStatut().toLowerCase(Locale.ROOT);
        String barColor;
        if (statut.contains("term") || statut.contains("finish") || statut.contains("fini")) {
            barColor = "#4ade80";
        } else if (statut.contains("plan") || statut.contains("sched")) {
            barColor = "#facc15";
        } else if (statut.contains("cours") || statut.contains("live")) {
            barColor = "#f472b6";
        } else {
            barColor = "#64748b";
        }
        Region bar = new Region();
        bar.setMinWidth(5);
        bar.setMaxWidth(5);
        bar.setMinHeight(70);
        bar.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 14 0 0 14;");

        // ── Date section ──
        String dateStr = m.getDateMatch() != null ? MATCH_DATE_FMT.format(m.getDateMatch()) : "—";
        Label dateLabel = new Label("🗓  " + dateStr);
        dateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        dateLabel.setMinWidth(155);

        // ── Team 1 ──
        String team1Name = equipeNames.getOrDefault(m.getEquipe1Id(), "Équipe #" + m.getEquipe1Id());
        Label team1 = new Label(team1Name);
        team1.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 15px; -fx-font-weight: 700;");
        team1.setMinWidth(160);
        team1.setAlignment(Pos.CENTER_RIGHT);

        // ── Score ──
        Integer s1 = m.getScoreTeam1();
        Integer s2 = m.getScoreTeam2();
        String scoreText = (s1 != null ? s1 : "–") + "  :  " + (s2 != null ? s2 : "–");

        boolean isFinished = statut.contains("term") || statut.contains("finish") || statut.contains("fini");
        String scoreBg = isFinished
                ? "-fx-background-color: linear-gradient(to right, #7c3aed, #db2777);"
                : "-fx-background-color: rgba(124,58,237,0.2);";

        Label scoreBadge = new Label(scoreText);
        scoreBadge.setStyle(scoreBg
                + " -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 800; "
                + "-fx-padding: 8 22; -fx-background-radius: 10;");
        scoreBadge.setMinWidth(100);
        scoreBadge.setAlignment(Pos.CENTER);

        // ── Team 2 ──
        String team2Name = equipeNames.getOrDefault(m.getEquipe2Id(), "Équipe #" + m.getEquipe2Id());
        Label team2 = new Label(team2Name);
        team2.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 15px; -fx-font-weight: 700;");
        team2.setMinWidth(160);

        // ── Status pill ──
        String pillText;
        String pillBg;
        String pillFg;
        if (statut.contains("term") || statut.contains("finish") || statut.contains("fini")) {
            pillText = "✓ Terminé";
            pillBg = "rgba(74,222,128,0.15)";
            pillFg = "#4ade80";
        } else if (statut.contains("plan") || statut.contains("sched")) {
            pillText = "⏱ Planifié";
            pillBg = "rgba(250,204,21,0.15)";
            pillFg = "#facc15";
        } else if (statut.contains("cours") || statut.contains("live")) {
            pillText = "● En cours";
            pillBg = "rgba(244,114,182,0.15)";
            pillFg = "#f472b6";
        } else {
            pillText = m.getStatut() != null ? m.getStatut() : "—";
            pillBg = "rgba(100,116,139,0.15)";
            pillFg = "#94a3b8";
        }
        Label pill = new Label(pillText);
        pill.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 11px; "
                        + "-fx-font-weight: 700; -fx-padding: 5 14; -fx-background-radius: 999;",
                pillBg, pillFg));
        pill.setMinWidth(90);
        pill.setAlignment(Pos.CENTER);

        // ── Spacers ──
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        // ── Match row ──
        HBox matchRow = new HBox(16, dateLabel, sp1, team1, scoreBadge, team2, sp2, pill);
        matchRow.setAlignment(Pos.CENTER);
        matchRow.setPadding(new Insets(14, 22, 14, 16));
        HBox.setHgrow(matchRow, Priority.ALWAYS);

        card.getChildren().addAll(bar, matchRow);
        return card;
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tournoiCatalog.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainContent.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("Catalogue des tournois");
            stage.show();
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px;");
            messageLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }

    private static LocalDate toLocalDate(java.util.Date d) {
        if (d == null) return null;
        if (d instanceof java.sql.Date sd) return sd.toLocalDate();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
