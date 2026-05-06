package tn.esprit.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import tn.esprit.entities.Jeu;
import tn.esprit.entities.Tournoi;
import tn.esprit.services.ServiceJeu;
import tn.esprit.services.ServiceTournoi;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class JeuxKpiController implements Initializable {

    @FXML private Label totalActifsLabel;
    @FXML private Label tauxInactifsLabel;
    @FXML private Label totalJeuxLabel;
    @FXML private Label topJeuLabel;
    @FXML private PieChart genrePieChart;
    @FXML private PieChart plateformePieChart;
    @FXML private BarChart<String, Number> topTournoisChart;
    @FXML private TableView<GameTournoiRow> topGamesTable;
    @FXML private TableColumn<GameTournoiRow, String> jeuCol;
    @FXML private TableColumn<GameTournoiRow, Number> tournoisCol;

    private final ServiceJeu serviceJeu = new ServiceJeu();
    private final ServiceTournoi serviceTournoi = new ServiceTournoi();
    private final Map<Integer, String> jeuNoms = new HashMap<>();
    private final List<Jeu> jeux = new ArrayList<>();
    private final List<Tournoi> tournois = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jeuCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().jeu()));
        tournoisCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().nbTournois()));
        loadData();
        renderKpis();
    }

    private void loadData() {
        jeux.clear();
        tournois.clear();
        jeuNoms.clear();
        try {
            jeux.addAll(serviceJeu.getAll());
            tournois.addAll(serviceTournoi.getAll());
            for (Jeu j : jeux) {
                jeuNoms.put(j.getId(), j.getNom());
            }
        } catch (SQLException e) {
            // keep empty state
        }
    }

    private void renderKpis() {
        totalJeuxLabel.setText(String.valueOf(jeux.size()));

        long actifs = jeux.stream().filter(j -> isStatus(j.getStatut(), "actif")).count();
        long inactifs = jeux.stream().filter(j -> isStatus(j.getStatut(), "inactif")).count();
        totalActifsLabel.setText(String.valueOf(actifs));
        double inactiveRate = jeux.isEmpty() ? 0 : (inactifs * 100.0 / jeux.size());
        tauxInactifsLabel.setText(String.format(Locale.FRANCE, "%.1f %%", inactiveRate));

        populateGenrePie();
        populatePlateformePie();
        Map<Integer, Integer> byGameTournois = tournoiCountByGame();
        populateTopTournois(byGameTournois);
        populateTopTable(byGameTournois);

        GameTournoiRow top = topGamesTable.getItems().isEmpty() ? null : topGamesTable.getItems().get(0);
        topJeuLabel.setText(top == null ? "—" : top.jeu() + " (" + top.nbTournois() + ")");
    }

    private void populateGenrePie() {
        Map<String, Integer> count = new HashMap<>();
        for (Jeu j : jeux) {
            String g = normalize(j.getGenre());
            count.put(g, count.getOrDefault(g, 0) + 1);
        }
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> e : count.entrySet()) {
            double pct = jeux.isEmpty() ? 0 : e.getValue() * 100.0 / jeux.size();
            data.add(new PieChart.Data(e.getKey() + " (" + String.format(Locale.FRANCE, "%.0f%%", pct) + ")", e.getValue()));
        }
        genrePieChart.setData(data);
    }

    private void populatePlateformePie() {
        Map<String, Integer> count = new HashMap<>();
        for (Jeu j : jeux) {
            for (String p : normalizePlatforms(j.getPlateforme())) {
                count.put(p, count.getOrDefault(p, 0) + 1);
            }
        }
        int total = count.values().stream().mapToInt(Integer::intValue).sum();
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> e : count.entrySet()) {
            double pct = total == 0 ? 0 : e.getValue() * 100.0 / total;
            data.add(new PieChart.Data(e.getKey() + " (" + String.format(Locale.FRANCE, "%.0f%%", pct) + ")", e.getValue()));
        }
        plateformePieChart.setData(data);
    }

    private Map<Integer, Integer> tournoiCountByGame() {
        Map<Integer, Integer> byGame = new HashMap<>();
        for (Tournoi t : tournois) {
            byGame.put(t.getJeuId(), byGame.getOrDefault(t.getJeuId(), 0) + 1);
        }
        return byGame;
    }

    private void populateTopTournois(Map<Integer, Integer> byGame) {
        topTournoisChart.getData().clear();
        BarChart.Series<String, Number> s = new BarChart.Series<>();
        byGame.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(8)
                .forEach(e -> s.getData().add(new BarChart.Data<>(jeuNoms.getOrDefault(e.getKey(), "Jeu #" + e.getKey()), e.getValue())));
        topTournoisChart.getData().add(s);
    }

    private void populateTopTable(Map<Integer, Integer> byGame) {
        List<GameTournoiRow> rows = byGame.entrySet().stream()
                .map(e -> new GameTournoiRow(jeuNoms.getOrDefault(e.getKey(), "Jeu #" + e.getKey()), e.getValue()))
                .sorted(Comparator.comparingInt(GameTournoiRow::nbTournois).reversed())
                .limit(10)
                .toList();
        topGamesTable.setItems(FXCollections.observableArrayList(rows));
    }

    private static boolean isStatus(String raw, String needle) {
        if (raw == null) return false;
        return raw.trim().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String normalize(String s) {
        if (s == null || s.isBlank()) return "N/A";
        return s.trim();
    }

    private static List<String> normalizePlatforms(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("N/A");
        }
        String[] parts = raw.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String v = p == null ? "" : p.trim();
            if (v.isEmpty()) {
                continue;
            }
            out.add(simplifyPlatform(v));
        }
        return out.isEmpty() ? List.of("N/A") : out;
    }

    private static String simplifyPlatform(String p) {
        String s = p.toLowerCase(Locale.ROOT);
        if (s.contains("playstation") || s.startsWith("ps")) return "PlayStation";
        if (s.contains("xbox")) return "Xbox";
        if (s.contains("switch")) return "Switch";
        if (s.contains("mobile") || s.contains("android") || s.contains("ios")) return "Mobile";
        if (s.contains("pc") || s.contains("windows")) return "PC";
        return p;
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/jeuxCatalog.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("Jeux");
            stage.show();
        } catch (IOException ignored) {
        }
    }

    public record GameTournoiRow(String jeu, int nbTournois) {}
}

