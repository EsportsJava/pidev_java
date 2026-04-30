package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.services.RiotApiService;
import tn.esprit.services.SteamApiService;
import tn.esprit.utils.ApiKeys;

import java.io.IOException;
import java.util.Locale;

/**
 * Formulaire après clic sur « RECHERCHER » depuis le dashboard Stats.
 */
public class StatsDetailController {

    public enum Mode {
        LEAGUE,
        VALORANT,
        STEAM
    }

    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private VBox riotSection;
    @FXML
    private VBox steamSection;
    @FXML
    private TextField riotNameField;
    @FXML
    private TextField riotTagField;
    @FXML
    private TextField steamGameField;
    @FXML
    private TextField steamIdField;
    @FXML
    private Label statusLabel;
    @FXML
    private Label steamLinkLabel;
    @FXML
    private Button submitPrimaryButton;
    @FXML
    private VBox playerStatsBox;

    private Mode mode = Mode.LEAGUE;

    @FXML
    public void initialize() {
        steamLinkLabel.setText("");
        hidePlayerStatsPanel();
        applyModeUi();
    }

    public void initMode(Mode m) {
        this.mode = m != null ? m : Mode.LEAGUE;
        Platform.runLater(this::applyModeUi);
    }

    private void applyModeUi() {
        if (mode == Mode.STEAM) {
            hidePlayerStatsPanel();
        }

        switch (mode) {
            case LEAGUE -> {
                pageTitleLabel.setText("LEAGUE OF LEGENDS");
                pageSubtitleLabel.setText(
                        "Consultez votre rang, vos stats et votre historique de matchs LoL.");
                riotSection.setVisible(true);
                riotSection.setManaged(true);
                steamSection.setVisible(false);
                steamSection.setManaged(false);
                styleSubmitRed();
            }
            case VALORANT -> {
                pageTitleLabel.setText("VALORANT");
                pageSubtitleLabel.setText(
                        "Analysez votre rang compétitif et vos performances Valorant.");
                riotSection.setVisible(true);
                riotSection.setManaged(true);
                steamSection.setVisible(false);
                steamSection.setManaged(false);
                styleSubmitRed();
            }
            case STEAM -> {
                pageTitleLabel.setText("CS2 / STEAM");
                pageSubtitleLabel.setText(
                        "Entrez votre SteamID64 pour afficher vos stats Counter-Strike 2, ou cherchez un lien Steam (Store).");
                riotSection.setVisible(false);
                riotSection.setManaged(false);
                steamSection.setVisible(true);
                steamSection.setManaged(true);
                if (submitPrimaryButton != null) {
                    submitPrimaryButton.setStyle(
                            "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a); "
                                    + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12;");
                }
                if (steamGameField != null && (steamGameField.getText() == null
                        || steamGameField.getText().isBlank())) {
                    steamGameField.setText("Counter-Strike 2");
                }
            }
        }
        statusLabel.setText("Prêt.");
        statusLabel.setStyle("-fx-text-fill: #cbd5e1;");
    }

    private void styleSubmitRed() {
        if (submitPrimaryButton != null) {
            submitPrimaryButton.setStyle(
                    "-fx-background-color: linear-gradient(to right, #e11d48, #dc2626); "
                            + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12;");
        }
    }

    @FXML
    private void handleSubmit() {
        if (mode == Mode.STEAM) {
            handleSteamSearch();
            return;
        }
        handleRiotSearch(mode == Mode.VALORANT ? "Valorant" : "League of Legends");
    }

    private void handleRiotSearch(String jeuSelection) {
        String apiKey = readRiotApiKey();
        if (apiKey.isBlank()) {
            hidePlayerStatsPanel();
            setError("Cle Riot manquante : renseignez ApiKeys.RIOT_API_KEY dans le code, ou variable RIOT_API_KEY, ou -DRIOT_API_KEY=...");
            return;
        }

        String gameName = safe(riotNameField.getText());
        String tagLine = safe(riotTagField.getText());
        if (gameName.isEmpty() || tagLine.isEmpty()) {
            hidePlayerStatsPanel();
            setError("Saisissez Riot ID (gameName) et tag (sans #).");
            return;
        }

        try {
            RiotApiService riotApiService = new RiotApiService(apiKey);
            JSONObject stats = riotApiService.fetchRecentStats(jeuSelection, gameName, tagLine);
            populatePlayerStats(stats);
            statusLabel.setStyle("-fx-text-fill: #22c55e;");
            statusLabel.setText("Statistiques Riot chargées.");
        } catch (Exception e) {
            hidePlayerStatsPanel();
            setError("Erreur Riot API: " + e.getMessage());
        }
    }

    private void populatePlayerStats(JSONObject stats) {
        if (playerStatsBox == null) {
            return;
        }
        playerStatsBox.getChildren().clear();
        playerStatsBox.setVisible(true);
        playerStatsBox.setManaged(true);

        String source = stats.optString("source", "");

        if ("lol".equals(source)) {
            renderLolProfileLayout(stats);
            return;
        }
        if ("valorant".equals(source)) {
            renderValorantProfileLayout(stats);
            return;
        }

        addSectionTitle(playerStatsBox, "Résumé joueur");

        String rName = stats.optString("riotGameName", "");
        String rTag = stats.optString("riotTagLine", "");
        addStatRow(playerStatsBox, "Riot ID", (rName.isEmpty() && rTag.isEmpty()) ? "—" : rName + "#" + rTag);
        addStatRow(playerStatsBox, "Routage API", stats.optString("riotRouting", "—"));
        String puuid = stats.optString("puuid", "");
        addStatRow(playerStatsBox, "PUUID", shorten(puuid, 36));

    }

    private void renderLolProfileLayout(JSONObject stats) {
        Button reset = new Button("← Nouvelle recherche");
        reset.setStyle("-fx-background-color: #252b3d; -fx-text-fill: #e2e8f0; -fx-font-size: 12; "
                + "-fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;");
        reset.setOnAction(e -> handleNewLolSearch());
        playerStatsBox.getChildren().add(reset);

        addLolProfileHeader(playerStatsBox, stats);
        addLolRankedSection(playerStatsBox, stats.optJSONArray("rankedEntries"));
        addLolMatchHistorySection(playerStatsBox, stats.optJSONArray("matchHistory"));
    }

    private void handleNewLolSearch() {
        hidePlayerStatsPanel();
        statusLabel.setText("Prêt pour une nouvelle recherche.");
        statusLabel.setStyle("-fx-text-fill: #cbd5e1;");
    }

    private void renderValorantProfileLayout(JSONObject stats) {
        Button reset = new Button("← Nouvelle recherche");
        reset.setStyle("-fx-background-color: #252b3d; -fx-text-fill: #e2e8f0; -fx-font-size: 12; "
                + "-fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;");
        reset.setOnAction(e -> handleNewLolSearch());
        playerStatsBox.getChildren().add(reset);

        JSONArray history = extractValorantHistory(stats);
        addValorantProfileHeader(playerStatsBox, stats, history);
        addValorantMatchHistorySection(playerStatsBox, history);
    }

    private static JSONArray extractValorantHistory(JSONObject stats) {
        JSONObject payload = stats.optJSONObject("payload");
        if (payload == null) {
            return new JSONArray();
        }
        JSONArray hist = payload.optJSONArray("history");
        if (hist == null) {
            hist = payload.optJSONArray("History");
        }
        return hist != null ? hist : new JSONArray();
    }

    private void addValorantProfileHeader(VBox parent, JSONObject stats, JSONArray history) {
        String gameName = stats.optString("riotGameName", "");
        String tagLine = stats.optString("riotTagLine", "");
        String routing = stats.optString("riotRouting", "—");
        int count = history == null ? 0 : history.length();

        HBox header = new HBox(18);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 18, 18));
        header.setStyle("-fx-background-color: #161b28; -fx-background-radius: 16; "
                + "-fx-border-color: #2a3142; -fx-border-radius: 16;");

        Label avatar = new Label("V");
        avatar.setStyle("-fx-text-fill: white; -fx-font-size: 28; -fx-font-weight: bold; "
                + "-fx-background-color: #dc2626; -fx-background-radius: 999; "
                + "-fx-min-width: 72; -fx-min-height: 72; -fx-alignment: center;");
        DropShadow glow = new DropShadow(18, Color.web("#ef4444"));
        glow.setSpread(0.15);
        avatar.setEffect(glow);

        VBox textCol = new VBox(8);
        HBox nameRow = new HBox(0);
        Label namePart = new Label(gameName.isEmpty() ? "—" : gameName);
        namePart.setStyle("-fx-text-fill: white; -fx-font-size: 22; -fx-font-weight: bold;");
        Label tagPart = new Label(tagLine.isEmpty() ? "" : "#" + tagLine);
        tagPart.setStyle("-fx-text-fill: #8892a0; -fx-font-size: 22; -fx-font-weight: bold;");
        nameRow.getChildren().addAll(namePart, tagPart);

        Label info = new Label("Shard " + routing + " • " + count + " matchs récents");
        info.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
        textCol.getChildren().addAll(nameRow, info);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(avatar, textCol, sp);
        parent.getChildren().add(header);
    }

    private void addValorantMatchHistorySection(VBox parent, JSONArray history) {
        Label title = new Label("📋 HISTORIQUE VALORANT");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 17; -fx-font-weight: bold; -fx-padding: 14 0 8 0;");
        parent.getChildren().add(title);

        if (history == null || history.length() == 0) {
            Label empty = new Label("Aucun match Valorant disponible pour ce compte.");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
            parent.getChildren().add(empty);
            return;
        }

        VBox list = new VBox(10);
        int max = Math.min(history.length(), 5);
        for (int i = 0; i < max; i++) {
            JSONObject row = history.optJSONObject(i);
            if (row == null) {
                continue;
            }
            list.getChildren().add(buildValorantMatchCard(row));
        }
        parent.getChildren().add(list);
    }

    private static Node buildValorantMatchCard(JSONObject m) {
        boolean hasWin = m.has("win");
        boolean win = m.optBoolean("win", false);
        String accent = !hasWin ? "#3b82f6" : (win ? "#22c55e" : "#dc2626");
        String outcome = !hasWin ? "MATCH" : (win ? "VICTOIRE" : "DÉFAITE");
        String outcomeColor = !hasWin ? "#60a5fa" : (win ? "#4ade80" : "#f87171");

        Region bar = new Region();
        bar.setPrefWidth(6);
        bar.setMinHeight(72);
        bar.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 4 0 0 4;");

        Label leftMain = new Label(outcome);
        leftMain.setStyle("-fx-text-fill: " + outcomeColor + "; -fx-font-size: 15; -fx-font-weight: bold;");
        String character = m.optString("character", "Agent");
        String map = m.optString("map", m.optString("mode", "VALORANT"));
        Label leftSub = new Label(character + " - " + map);
        leftSub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
        VBox leftText = new VBox(4, leftMain, leftSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean hasKda = m.has("kills") || m.has("deaths") || m.has("assists");
        int k = m.optInt("kills", 0);
        int d = m.optInt("deaths", 0);
        int a = m.optInt("assists", 0);
        String rounds = m.has("roundsWon")
                ? m.optInt("roundsWon", 0) + "-" + m.optInt("roundsLost", 0)
                : "—";
        String score = m.has("score") ? String.valueOf(m.optInt("score", 0)) : "—";
        Label kdaMain = new Label(hasKda ? (k + " / " + d + " / " + a) : "— / — / —");
        kdaMain.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        Label kdaSub = new Label("ACS/Score: " + score + " • Rounds: " + rounds);
        kdaSub.setStyle("-fx-text-fill: #8892a0; -fx-font-size: 12;");
        VBox rightCol = new VBox(3, kdaMain, kdaSub);
        rightCol.setAlignment(Pos.CENTER_RIGHT);

        HBox inner = new HBox(14, leftText, spacer, rightCol);
        inner.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inner, Priority.ALWAYS);

        HBox card = new HBox(0, bar, inner);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 10));
        card.setStyle("-fx-background-color: #121928; -fx-background-radius: 12; -fx-border-color: #2a3548; -fx-border-radius: 12;");
        HBox.setHgrow(inner, Priority.ALWAYS);
        return card;
    }

    private void addLolProfileHeader(VBox parent, JSONObject stats) {
        String gameName = stats.optString("riotGameName", "");
        String tagLine = stats.optString("riotTagLine", "");
        int level = stats.optInt("summonerLevel", 0);
        JSONArray hist = stats.optJSONArray("matchHistory");
        String champ = "Aatrox";
        if (hist != null && hist.length() > 0) {
            JSONObject first = hist.optJSONObject(0);
            if (first != null && first.has("championName")) {
                champ = first.optString("championName", champ);
            }
        }

        HBox header = new HBox(18);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 18, 18));
        header.setStyle("-fx-background-color: #161b28; -fx-background-radius: 16; "
                + "-fx-border-color: #2a3142; -fx-border-radius: 16;");

        ImageView avatar = new ImageView(new Image(ddragonChampionUrl(champ), 88, 88, true, true, true));
        avatar.setPreserveRatio(true);
        DropShadow glow = new DropShadow(18, Color.web("#ef4444"));
        glow.setSpread(0.15);
        avatar.setEffect(glow);

        VBox nameCol = new VBox(10);
        HBox nameRow = new HBox(0);
        Label namePart = new Label(gameName.isEmpty() ? "—" : gameName);
        namePart.setStyle("-fx-text-fill: white; -fx-font-size: 22; -fx-font-weight: bold;");
        Label tagPart = new Label(tagLine.isEmpty() ? "" : "#" + tagLine);
        tagPart.setStyle("-fx-text-fill: #8892a0; -fx-font-size: 22; -fx-font-weight: bold;");
        nameRow.getChildren().addAll(namePart, tagPart);

        Label lvl = new Label(level > 0 ? "Niveau " + level : "Niveau —");
        lvl.setStyle("-fx-text-fill: white; -fx-background-color: #dc2626; -fx-background-radius: 20; "
                + "-fx-padding: 5 14; -fx-font-size: 13; -fx-font-weight: bold;");
        nameCol.getChildren().addAll(nameRow, lvl);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(avatar, nameCol, sp);
        parent.getChildren().add(header);
    }

    private void addLolRankedSection(VBox parent, JSONArray rankedEntries) {
        Label title = new Label("🏆 CLASSEMENT RANKED");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 17; -fx-font-weight: bold; -fx-padding: 16 0 10 0;");
        parent.getChildren().add(title);

        JSONObject flex = findQueueEntry(rankedEntries, "RANKED_FLEX_SR");
        JSONObject solo = findQueueEntry(rankedEntries, "RANKED_SOLO_5x5");

        VBox c1 = buildRankedCard("Flex 5v5", flex);
        VBox c2 = buildRankedCard("Solo/Duo", solo);
        HBox row = new HBox(14, c1, c2);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(c1, Priority.ALWAYS);
        HBox.setHgrow(c2, Priority.ALWAYS);
        parent.getChildren().add(row);
    }

    private static JSONObject findQueueEntry(JSONArray entries, String queueType) {
        if (entries == null) {
            return null;
        }
        for (int i = 0; i < entries.length(); i++) {
            JSONObject o = entries.optJSONObject(i);
            if (o != null && queueType.equals(o.optString("queueType", ""))) {
                return o;
            }
        }
        return null;
    }

    private static VBox buildRankedCard(String queueLabel, JSONObject entry) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18, 16, 16, 16));
        card.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 14; "
                + "-fx-border-color: #2d3548; -fx-border-radius: 14;");

        Label l0 = new Label(queueLabel);
        l0.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");

        if (entry == null || entry.optString("tier", "").isBlank()) {
            Label nk = new Label("Non classé");
            nk.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: bold;");
            Label sub = new Label("— LP");
            sub.setStyle("-fx-text-fill: #8892a0; -fx-font-size: 13;");
            Label foot = new Label("—");
            foot.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11; -fx-padding: 10 0 0 0;");
            card.getChildren().addAll(l0, nk, sub, foot);
            return card;
        }

        String tier = entry.optString("tier", "");
        String rank = entry.optString("rank", "");
        String rankLine = tier;
        if (!rank.isBlank()) {
            rankLine = tier + " " + rank;
        }
        Label big = new Label(rankLine);
        big.setStyle("-fx-text-fill: white; -fx-font-size: 22; -fx-font-weight: bold;");

        int lp = entry.optInt("leaguePoints", 0);
        Label lpLb = new Label(lp + " LP");
        lpLb.setStyle("-fx-text-fill: #8892a0; -fx-font-size: 13;");

        int wins = entry.optInt("wins", 0);
        int losses = entry.optInt("losses", 0);
        int total = wins + losses;
        String wr = total == 0 ? "0" : String.format(Locale.FRANCE, "%.0f", wins * 100.0 / total);
        Label footer = new Label(wins + "V " + losses + "D - " + wr + "% WR");
        footer.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12; -fx-padding: 10 0 0 0;");
        footer.setWrapText(true);

        card.getChildren().addAll(l0, big, lpLb, footer);
        return card;
    }

    private static final String DDRAGON_VERSION = "15.7.1";

    private void addLolMatchHistorySection(VBox parent, JSONArray matchHistory) {
        if (matchHistory == null || matchHistory.length() == 0) {
            Label empty = new Label("Aucun match detaille (IDs disponibles mais chargement des parties impossible).");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
            parent.getChildren().add(empty);
            return;
        }
        Label title = new Label("📋 HISTORIQUE DES MATCHS");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 17; -fx-font-weight: bold; -fx-padding: 14 0 8 0;");
        parent.getChildren().add(title);

        VBox list = new VBox(10);
        for (int i = 0; i < matchHistory.length(); i++) {
            JSONObject row = matchHistory.optJSONObject(i);
            if (row == null || !row.has("championName")) {
                continue;
            }
            list.getChildren().add(buildLolMatchCard(row));
        }
        parent.getChildren().add(list);
    }

    private static Node buildLolMatchCard(JSONObject m) {
        boolean win = m.optBoolean("win", false);
        String accent = win ? "#22c55e" : "#dc2626";
        String outcomeColor = win ? "#4ade80" : "#f87171";

        Region bar = new Region();
        bar.setPrefWidth(6);
        bar.setMinHeight(72);
        bar.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 4 0 0 4;");

        ImageView avatar = new ImageView();
        avatar.setFitWidth(56);
        avatar.setFitHeight(56);
        avatar.setPreserveRatio(true);
        String champ = m.optString("championName", "Aatrox");
        avatar.setImage(new Image(ddragonChampionUrl(champ), 56, 56, true, true, true));

        Label outcome = new Label(win ? "VICTOIRE" : "DÉFAITE");
        outcome.setStyle("-fx-text-fill: " + outcomeColor + "; -fx-font-size: 15; -fx-font-weight: bold;");
        String mode = m.optString("gameMode", "CLASSIC");
        Label line2 = new Label(champ + " - " + mode);
        line2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
        VBox leftText = new VBox(4, outcome, line2);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        int k = m.optInt("kills", 0);
        int d = m.optInt("deaths", 0);
        int a = m.optInt("assists", 0);
        Label kdaMain = new Label(k + " / " + d + " / " + a);
        kdaMain.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        Label kdaSub = new Label("KDA : " + formatKdaRatio(k, d, a));
        kdaSub.setStyle("-fx-text-fill: #8892a0; -fx-font-size: 12;");
        VBox rightCol = new VBox(3, kdaMain, kdaSub);
        rightCol.setAlignment(Pos.CENTER_RIGHT);

        HBox inner = new HBox(14, avatar, leftText, spacer, rightCol);
        inner.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inner, Priority.ALWAYS);

        HBox card = new HBox(0, bar, inner);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 10));
        card.setStyle("-fx-background-color: #121928; -fx-background-radius: 12; -fx-border-color: #2a3548; -fx-border-radius: 12;");
        HBox.setHgrow(inner, Priority.ALWAYS);
        return card;
    }

    private static String ddragonChampionUrl(String championName) {
        if (championName == null || championName.isBlank()) {
            championName = "Aatrox";
        }
        return "https://ddragon.leagueoflegends.com/cdn/" + DDRAGON_VERSION + "/img/champion/"
                + championName + ".png";
    }

    private static String formatKdaRatio(int k, int d, int a) {
        if (d == 0) {
            return "Perfect";
        }
        double v = (k + a) / (double) d;
        return String.format(Locale.US, "%.1f", v);
    }

    private static void addSectionTitle(VBox box, String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 15; -fx-font-weight: bold; -fx-padding: 8 0 4 0;");
        box.getChildren().add(l);
    }

    private static void addStatRow(VBox box, String label, String value) {
        HBox row = new HBox(14);
        row.setPadding(new Insets(2, 0, 2, 0));
        Label l1 = new Label(label + " :");
        l1.setMinWidth(160);
        l1.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
        Label l2 = new Label(value == null ? "—" : value);
        l2.setWrapText(true);
        l2.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13;");
        row.getChildren().addAll(l1, l2);
        box.getChildren().add(row);
    }

    private static void addStatRowMultiline(VBox box, String label, String value) {
        HBox row = new HBox(14);
        row.setPadding(new Insets(2, 0, 2, 0));
        Label l1 = new Label(label + " :");
        l1.setMinWidth(160);
        l1.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
        Label l2 = new Label(value == null ? "—" : value);
        l2.setWrapText(true);
        l2.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12;");
        row.getChildren().addAll(l1, l2);
        box.getChildren().add(row);
    }

    private static String shorten(String s, int maxLen) {
        if (s == null || s.isEmpty()) {
            return "—";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen - 1) + "…";
    }

    private void hidePlayerStatsPanel() {
        if (playerStatsBox == null) {
            return;
        }
        playerStatsBox.getChildren().clear();
        playerStatsBox.setVisible(false);
        playerStatsBox.setManaged(false);
    }

    private void handleSteamSearch() {
        hidePlayerStatsPanel();
        String selectedGame = safe(steamGameField.getText());
        String steamId = steamIdField != null ? safe(steamIdField.getText()) : "";
        try {
            SteamApiService steamApiService = new SteamApiService(readSteamApiKey());
            // 1) Si SteamID64 saisi → stats CS2
            if (!steamId.isBlank()) {
                if (!steamId.matches("^\\d{17}$")) {
                    setError("SteamID64 invalide. Il doit contenir exactement 17 chiffres (ex: 7656119XXXXXXXXXX). "
                            + "Ne collez pas la clé Steam API ici.");
                    return;
                }
                JSONObject profile = steamApiService.getPlayerSummary(steamId);
                JSONObject cs2 = steamApiService.getCs2Stats(steamId);
                renderCs2Stats(profile, cs2, steamId);
                statusLabel.setStyle("-fx-text-fill: #22c55e;");
                statusLabel.setText("Stats CS2 chargées.");
                return;
            }

            // 2) Sinon → lien store
            if (selectedGame.isEmpty()) {
                setError("Saisissez le SteamID64 (pour stats CS2) ou le nom du jeu (Store).");
                return;
            }
            String url = steamApiService.findOfficialStoreUrl(selectedGame);
            if (url.isBlank()) {
                steamLinkLabel.setText("Aucun lien trouvé pour « " + selectedGame + " ».");
                statusLabel.setStyle("-fx-text-fill: #f59e0b;");
                statusLabel.setText("Steam: aucun résultat.");
                return;
            }
            steamLinkLabel.setText(url);
            statusLabel.setStyle("-fx-text-fill: #22c55e;");
            statusLabel.setText("Lien Steam récupéré.");
        } catch (Exception e) {
            setError("Erreur Steam: " + e.getMessage());
        }
    }

    private void renderCs2Stats(JSONObject profile, JSONObject cs2, String steamId) {
        if (playerStatsBox == null) {
            return;
        }
        playerStatsBox.getChildren().clear();
        playerStatsBox.setVisible(true);
        playerStatsBox.setManaged(true);

        Button reset = new Button("← Nouvelle recherche");
        reset.setStyle("-fx-background-color: #252b3d; -fx-text-fill: #e2e8f0; -fx-font-size: 12; "
                + "-fx-background-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;");
        reset.setOnAction(e -> handleNewLolSearch());
        playerStatsBox.getChildren().add(reset);

        // Header (profil Steam + CS2)
        HBox header = new HBox(18);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 18, 18, 18));
        header.setStyle("-fx-background-color: #161b28; -fx-background-radius: 16; "
                + "-fx-border-color: #2a3142; -fx-border-radius: 16;");

        Node avatarNode = buildSteamAvatar(profile);

        VBox textCol = new VBox(8);
        String persona = extractSteamPersonaName(profile);
        Label title = new Label(persona.isBlank() ? "Counter-Strike 2" : persona);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22; -fx-font-weight: bold;");
        Label sub = new Label("Counter-Strike 2 • SteamID64: " + steamId);
        sub.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13;");
        textCol.getChildren().addAll(title, sub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(avatarNode, textCol, sp);
        playerStatsBox.getChildren().add(header);

        // KPI cards (Aperçu)
        int kills = cs2.optInt("total_kills", 0);
        int deaths = cs2.optInt("total_deaths", 0);
        int wins = cs2.optInt("total_wins", 0);
        int matches = cs2.optInt("total_matches_played", 0);
        double kd = deaths == 0 ? kills : (kills / (double) deaths);
        int rounds = cs2.optInt("total_rounds_played", 0);
        int mvps = cs2.optInt("total_mvps", 0);
        int hs = cs2.optInt("total_kills_headshot", 0);
        int shots = cs2.optInt("total_shots_fired", 0);
        int hits = cs2.optInt("total_shots_hit", 0);
        int dmg = cs2.optInt("total_damage_done", 0);
        int timeSec = cs2.optInt("total_time_played", 0);
        int plants = cs2.optInt("total_planted_bombs", 0);
        int defuses = cs2.optInt("total_defused_bombs", 0);

        double hsPct = kills == 0 ? 0 : (hs * 100.0 / kills);
        double accPct = shots == 0 ? 0 : (hits * 100.0 / shots);
        double adr = rounds == 0 ? 0 : (dmg / (double) rounds);

        HBox row = new HBox(14,
                buildMiniKpi("Kills", String.valueOf(kills)),
                buildMiniKpi("Deaths", String.valueOf(deaths)),
                buildMiniKpi("K/D", String.format(Locale.US, "%.2f", kd)),
                buildMiniKpi("Wins", String.valueOf(wins)),
                buildMiniKpi("Matchs", String.valueOf(matches)),
                buildMiniKpi("Heures", formatHours(timeSec))
        );
        row.setAlignment(Pos.CENTER_LEFT);
        playerStatsBox.getChildren().add(row);

        Label secTitle = new Label("📊 Détails CS2");
        secTitle.setStyle("-fx-text-fill: white; -fx-font-size: 17; -fx-font-weight: bold; -fx-padding: 12 0 6 0;");
        playerStatsBox.getChildren().add(secTitle);

        HBox row2 = new HBox(14,
                buildMiniKpi("Rounds", String.valueOf(rounds)),
                buildMiniKpi("MVP", String.valueOf(mvps)),
                buildMiniKpi("HS%", String.format(Locale.US, "%.1f%%", hsPct)),
                buildMiniKpi("Accuracy", String.format(Locale.US, "%.1f%%", accPct)),
                buildMiniKpi("ADR", String.format(Locale.US, "%.0f", adr)),
                buildMiniKpi("Bombes", plants + " / " + defuses)
        );
        row2.setAlignment(Pos.CENTER_LEFT);
        playerStatsBox.getChildren().add(row2);
    }

    private static VBox buildMiniKpi(String label, String value) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 14; "
                + "-fx-border-color: #2d3548; -fx-border-radius: 14;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");
        Label v = new Label(value == null ? "—" : value);
        v.setStyle("-fx-text-fill: white; -fx-font-size: 20; -fx-font-weight: bold;");
        card.getChildren().addAll(l, v);
        return card;
    }

    private static String formatHours(int seconds) {
        if (seconds <= 0) {
            return "0";
        }
        double h = seconds / 3600.0;
        return String.format(Locale.US, "%.1f", h);
    }

    private static String extractSteamPersonaName(JSONObject profile) {
        if (profile == null) {
            return "";
        }
        JSONObject response = profile.optJSONObject("response");
        if (response == null) {
            return "";
        }
        org.json.JSONArray players = response.optJSONArray("players");
        if (players == null || players.length() == 0) {
            return "";
        }
        JSONObject p = players.optJSONObject(0);
        return p == null ? "" : p.optString("personaname", "");
    }

    private static Node buildSteamAvatar(JSONObject profile) {
        String avatarUrl = "";
        if (profile != null) {
            JSONObject response = profile.optJSONObject("response");
            org.json.JSONArray players = response != null ? response.optJSONArray("players") : null;
            JSONObject p = (players != null && players.length() > 0) ? players.optJSONObject(0) : null;
            if (p != null) {
                avatarUrl = p.optString("avatarfull", "");
            }
        }

        if (avatarUrl == null || avatarUrl.isBlank()) {
            Label fallback = new Label("CS");
            fallback.setStyle("-fx-text-fill: white; -fx-font-size: 22; -fx-font-weight: bold; "
                    + "-fx-background-color: #16a34a; -fx-background-radius: 999; "
                    + "-fx-min-width: 72; -fx-min-height: 72; -fx-alignment: center;");
            return fallback;
        }

        ImageView iv = new ImageView(new Image(avatarUrl, 72, 72, true, true, true));
        iv.setFitWidth(72);
        iv.setFitHeight(72);
        iv.setPreserveRatio(true);
        DropShadow glow = new DropShadow(16, Color.web("#22c55e"));
        glow.setSpread(0.12);
        iv.setEffect(glow);
        return iv;
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/stats.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1280, 720));
        stage.setTitle("Stats – dashboard");
        stage.show();
    }

    private void setError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #ef4444;");
        statusLabel.setText(msg);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim().replace("#", "");
    }

    /**
     * Ordre : {@link ApiKeys} (code local) d'abord, puis {@code -D}, puis env.
     * Evite qu'une VM option ou un vieux setx ne masque une cle fraiche dans ApiKeys.java.
     */
    private static String readRiotApiKey() {
        String fromClass = ApiKeys.RIOT_API_KEY == null ? "" : ApiKeys.RIOT_API_KEY.trim();
        if (!fromClass.isBlank()) {
            return fromClass;
        }
        String fromProperty = System.getProperty("RIOT_API_KEY");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromEnv = System.getenv("RIOT_API_KEY");
        return fromEnv == null ? "" : fromEnv.trim();
    }

    private static String readSteamApiKey() {
        String fromClass = ApiKeys.STEAM_API_KEY == null ? "" : ApiKeys.STEAM_API_KEY.trim();
        if (!fromClass.isBlank()) {
            return fromClass;
        }
        String fromProperty = System.getProperty("STEAM_API_KEY");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromEnv = System.getenv("STEAM_API_KEY");
        return fromEnv == null ? "" : fromEnv.trim();
    }
}
