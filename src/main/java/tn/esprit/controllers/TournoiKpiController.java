package tn.esprit.controllers;

import javafx.beans.property.SimpleDoubleProperty;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import tn.esprit.entities.Jeu;
import tn.esprit.entities.Tournoi;
import tn.esprit.services.ServiceJeu;
import tn.esprit.services.ServiceTournoi;
import tn.esprit.services.ServiceTournoiInscription;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class TournoiKpiController implements Initializable {

    @FXML
    private Label totalGlobalLabel;
    @FXML
    private Label totalPeriodeLabel;
    @FXML
    private Label ouvertsLabel;
    @FXML
    private Label remplissageLabel;
    @FXML
    private Label cagnotteTotalLabel;
    @FXML
    private Label cagnotteMoyenneLabel;
    @FXML
    private ComboBox<String> periodeCombo;
    @FXML
    private PieChart statutPieChart;
    @FXML
    private BarChart<String, Number> fraisTypeChart;
    @FXML
    private TableView<GameRevenueRow> topJeuxTable;
    @FXML
    private TableColumn<GameRevenueRow, String> jeuCol;
    @FXML
    private TableColumn<GameRevenueRow, Number> montantCol;

    private final ServiceTournoi serviceTournoi = new ServiceTournoi();
    private final ServiceJeu serviceJeu = new ServiceJeu();
    private final ServiceTournoiInscription serviceTournoiInscription = new ServiceTournoiInscription();
    private final Map<Integer, String> jeuNoms = new HashMap<>();
    private final List<Tournoi> allTournois = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        jeuCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().jeu()));
        montantCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().montant()));
        montantCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatMoney(item.doubleValue()));
            }
        });

        periodeCombo.setItems(FXCollections.observableArrayList(
                "Tous", "30 derniers jours", "90 derniers jours", "365 derniers jours"
        ));
        periodeCombo.getSelectionModel().selectFirst();
        periodeCombo.setOnAction(e -> refreshKpis());
        applyVisualTheme();

        loadData();
        refreshKpis();
    }

    private void loadData() {
        allTournois.clear();
        jeuNoms.clear();
        try {
            for (Jeu j : serviceJeu.getAll()) {
                jeuNoms.put(j.getId(), j.getNom());
            }
            allTournois.addAll(serviceTournoi.getAll());
        } catch (SQLException e) {
            totalGlobalLabel.setText("Erreur");
        }
    }

    private void refreshKpis() {
        List<Tournoi> filtered = filterByPeriod(allTournois, periodeCombo.getValue());

        totalGlobalLabel.setText(String.valueOf(allTournois.size()));
        totalPeriodeLabel.setText(String.valueOf(filtered.size()));
        ouvertsLabel.setText(String.valueOf(filtered.stream().filter(t -> isOpenStatus(t.getStatut())).count()));

        remplissageLabel.setText(buildFillForecast(filtered));

        double cagTot = filtered.stream().mapToDouble(Tournoi::getCagnotte).sum();
        cagnotteTotalLabel.setText(formatMoney(cagTot));
        cagnotteMoyenneLabel.setText(filtered.isEmpty() ? "0 TND" : formatMoney(cagTot / filtered.size()));

        populateStatusPie(filtered);
        populateFraisByType(filtered);
        populateTopJeux(filtered);
    }

    private void populateStatusPie(List<Tournoi> list) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("Planifié", 0);
        counts.put("En cours", 0);
        counts.put("Terminé", 0);
        counts.put("Annulé", 0);

        for (Tournoi t : list) {
            String s = normalizeStatusBucket(t.getStatut());
            counts.put(s, counts.getOrDefault(s, 0) + 1);
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            data.add(new PieChart.Data(e.getKey(), e.getValue()));
        }
        statutPieChart.setData(data);
        stylePieSlices();
    }

    private void populateFraisByType(List<Tournoi> list) {
        Map<String, double[]> agg = new HashMap<>(); // [sum, count]
        for (Tournoi t : list) {
            String type = normalizeType(t.getType());
            double[] box = agg.computeIfAbsent(type, k -> new double[]{0, 0});
            box[0] += t.getFraisInscription();
            box[1] += 1;
        }

        fraisTypeChart.getData().clear();
        BarChart.Series<String, Number> s = new BarChart.Series<>();
        s.setName("Frais moyen");
        for (Map.Entry<String, double[]> e : agg.entrySet()) {
            double avg = e.getValue()[1] == 0 ? 0 : e.getValue()[0] / e.getValue()[1];
            s.getData().add(new BarChart.Data<>(e.getKey(), avg));
        }
        fraisTypeChart.getData().add(s);
        styleBarSeries(s);
    }

    private void populateTopJeux(List<Tournoi> list) {
        Map<Integer, Double> byJeu = new HashMap<>();
        for (Tournoi t : list) {
            byJeu.merge(t.getJeuId(), t.getCagnotte(), Double::sum);
        }
        List<GameRevenueRow> rows = byJeu.entrySet().stream()
                .map(e -> new GameRevenueRow(jeuNoms.getOrDefault(e.getKey(), "Jeu #" + e.getKey()), e.getValue()))
                .sorted(Comparator.comparingDouble(GameRevenueRow::montant).reversed())
                .limit(10)
                .toList();
        topJeuxTable.setItems(FXCollections.observableArrayList(rows));
    }

    private static List<Tournoi> filterByPeriod(List<Tournoi> source, String period) {
        if (period == null || period.equalsIgnoreCase("Tous")) {
            return source;
        }
        int days = switch (period) {
            case "30 derniers jours" -> 30;
            case "90 derniers jours" -> 90;
            case "365 derniers jours" -> 365;
            default -> 0;
        };
        if (days <= 0) {
            return source;
        }
        LocalDate minDate = LocalDate.now().minusDays(days);
        return source.stream()
                .filter(t -> {
                    if (t.getDateDebut() == null) {
                        return false;
                    }
                    LocalDate d = toLocalDate(t.getDateDebut());
                    return !d.isBefore(minDate);
                })
                .toList();
    }

    private static LocalDate toLocalDate(java.util.Date d) {
        if (d == null) {
            return LocalDate.MIN;
        }
        if (d instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static boolean isOpenStatus(String statut) {
        if (statut == null) {
            return false;
        }
        String s = statut.trim().toLowerCase(Locale.ROOT);
        return s.contains("planifi") || s.contains("cours") || s.contains("ouvert");
    }

    private static String normalizeStatusBucket(String raw) {
        if (raw == null) {
            return "Planifié";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.contains("annul")) {
            return "Annulé";
        }
        if (s.contains("termin")) {
            return "Terminé";
        }
        if (s.contains("cours")) {
            return "En cours";
        }
        return "Planifié";
    }

    private static String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "N/A";
        }
        return raw.trim();
    }

    private static String formatMoney(double amount) {
        if (Math.floor(amount) == amount) {
            return ((long) amount) + " TND";
        }
        return String.format(Locale.FRANCE, "%.2f TND", amount);
    }

    private String buildFillForecast(List<Tournoi> filtered) {
        try {
            int eligible = 0;
            int predictedFull = 0;
            int relanceNeeded = 0;
            LocalDate today = LocalDate.now();

            for (Tournoi t : filtered) {
                if (!isOpenStatus(t.getStatut()) || t.getMaxParticipants() <= 0 || t.getDateInscriptionLimite() == null) {
                    continue;
                }
                eligible++;
                ServiceTournoiInscription.TournoiInscriptionSnapshot snap =
                        serviceTournoiInscription.getSnapshotByTournoi(t.getId());

                int current = Math.max(0, snap.count());
                int max = Math.max(1, t.getMaxParticipants());
                int remaining = Math.max(0, max - current);
                if (remaining == 0) {
                    predictedFull++;
                    continue;
                }

                LocalDate deadline = toLocalDate(t.getDateInscriptionLimite());
                long daysLeft = Math.max(0, ChronoUnit.DAYS.between(today, deadline));
                if (daysLeft == 0) {
                    relanceNeeded++;
                    continue;
                }

                double pacePerDay = estimatePacePerDay(current, snap.firstInscriptionAt(), today);
                int projectedFinal = (int) Math.round(current + (pacePerDay * daysLeft));
                if (projectedFinal >= max) {
                    predictedFull++;
                } else {
                    relanceNeeded++;
                }
            }

            if (eligible == 0) {
                return "N/A (aucun tournoi éligible)";
            }
            double ratio = (predictedFull * 100.0) / eligible;
            return String.format(
                    Locale.FRANCE,
                    "Prévision complet avant deadline: %d/%d (%.0f%%) | Relance: %d",
                    predictedFull, eligible, ratio, relanceNeeded
            );
        } catch (SQLException e) {
            return "Prévision indisponible: " + e.getMessage();
        }
    }

    private static double estimatePacePerDay(int current, LocalDateTime firstInscriptionAt, LocalDate today) {
        if (current <= 0 || firstInscriptionAt == null) {
            return 0.0;
        }
        LocalDate first = firstInscriptionAt.toLocalDate();
        long activeDays = Math.max(1, ChronoUnit.DAYS.between(first, today) + 1);
        return current / (double) activeDays;
    }

    private void applyVisualTheme() {
        statutPieChart.setStyle("-fx-background-color: transparent; -fx-padding: 8;");
        fraisTypeChart.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-legend-visible: false;"
                        + "-fx-horizontal-grid-lines-visible: true;"
                        + "-fx-vertical-grid-lines-visible: false;"
                        + "-fx-alternative-row-fill-visible: false;");

        topJeuxTable.setStyle(
                "-fx-background-color: #111a33;"
                        + "-fx-control-inner-background: #111a33;"
                        + "-fx-table-cell-border-color: #26365f;"
                        + "-fx-padding: 6;");
        topJeuxTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void stylePieSlices() {
        String[] colors = {"#7c3aed", "#3b82f6", "#22c55e", "#ef4444"};
        int i = 0;
        for (PieChart.Data d : statutPieChart.getData()) {
            final String color = colors[i % colors.length];
            i++;
            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-pie-color: " + color + ";");
            } else {
                d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-pie-color: " + color + ";");
                    }
                });
            }
        }
    }

    private void styleBarSeries(BarChart.Series<String, Number> series) {
        for (BarChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-bar-fill: linear-gradient(to top, #7c3aed, #a855f7);");
            } else {
                d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-bar-fill: linear-gradient(to top, #7c3aed, #a855f7);");
                    }
                });
            }
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/tournoiDashboard.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle("Tournois");
            stage.show();
        } catch (IOException e) {
            // no-op
        }
    }

    public record GameRevenueRow(String jeu, double montant) {}
}

