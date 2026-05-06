package tn.esprit.controllers;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import tn.esprit.entities.Jeu;
import tn.esprit.services.ServiceJeu;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Panneau liste des jeux (cartes). CRUD actif sauf si {@link #setReadOnly(boolean)} est appelé avec {@code true}
 * (page catalogue avec navbar).
 */
public class JeuxPanelController implements Initializable {

    private static final double CARD_W = 350;
    private static final double IMG_H = 132;
    private static final String CARD_STYLE =
            "-fx-background-color: #151f38;"
                    + "-fx-background-radius: 12;"
                    + "-fx-border-color: rgba(124,58,237,0.45);"
                    + "-fx-border-radius: 12;"
                    + "-fx-border-width: 1;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 14, 0.2, 0, 4);";
    private static final String CARD_STYLE_HOVER =
            "-fx-background-color: #1a2747;"
                    + "-fx-background-radius: 12;"
                    + "-fx-border-color: rgba(167,139,250,0.75);"
                    + "-fx-border-radius: 12;"
                    + "-fx-border-width: 1.3;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.62), 20, 0.24, 0, 6);";

    @FXML
    private FlowPane cardsContainer;
    @FXML
    private ScrollPane cardsScrollPane;
    @FXML
    private Label messageLabel;
    @FXML
    private Label totalJeuxLabel;
    @FXML
    private Label genresCountLabel;
    @FXML
    private Label plateformesCountLabel;
    @FXML
    private Label actifsCountLabel;
    @FXML
    private Button newGameButton;
    @FXML
    private Button allFilterBtn;
    @FXML
    private Button activeFilterBtn;
    @FXML
    private Button fpsFilterBtn;
    @FXML
    private Button brFilterBtn;
    @FXML
    private Button sportFilterBtn;

    private final ServiceJeu serviceJeu = new ServiceJeu();
    private Image coverPlaceholder;
    private boolean readOnly;
    private final List<Jeu> allJeux = new ArrayList<>();
    private final Map<String, Image> imageCache = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        URL imgUrl = getClass().getResource("/images/game.png");
        if (imgUrl != null) {
            coverPlaceholder = new Image(imgUrl.toExternalForm(), CARD_W, IMG_H, false, true);
        }
        cardsContainer.setAlignment(Pos.TOP_CENTER);
        cardsContainer.prefWrapLengthProperty().bind(Bindings.min(cardsScrollPane.widthProperty().subtract(40), 1082));
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        if (newGameButton != null) {
            newGameButton.setVisible(!readOnly);
            newGameButton.setManaged(!readOnly);
        }
        loadJeux();
    }

    private void loadJeux() {
        try {
            allJeux.clear();
            allJeux.addAll(serviceJeu.getAll());
            updateStats(allJeux);
            setActiveFilterButton(allFilterBtn);
            showJeux(allJeux);
        } catch (SQLException e) {
            showError("Impossible de charger les jeux : " + e.getMessage());
        }
    }

    private void showJeux(List<Jeu> list) {
        messageLabel.setText("");
        cardsContainer.getChildren().clear();
        if (list.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: #cbd5e1;");
            messageLabel.setText("Aucun jeu pour ce filtre.");
            return;
        }
        for (Jeu j : list) {
            cardsContainer.getChildren().add(buildGameCard(j));
        }
    }

    private void updateStats(List<Jeu> list) {
        totalJeuxLabel.setText(String.valueOf(list.size()));
        Set<String> genres = new HashSet<>();
        Set<String> plateformes = new HashSet<>();
        int actifs = 0;
        for (Jeu j : list) {
            if (j.getGenre() != null && !j.getGenre().isBlank()) {
                genres.add(j.getGenre().trim().toLowerCase());
            }
            if (j.getPlateforme() != null && !j.getPlateforme().isBlank()) {
                plateformes.add(j.getPlateforme().trim().toLowerCase());
            }
            String statut = j.getStatut() == null ? "" : j.getStatut().toLowerCase();
            if (statut.contains("actif") || statut.contains("dispo") || statut.contains("ouvert")) {
                actifs++;
            }
        }
        genresCountLabel.setText(String.valueOf(genres.size()));
        plateformesCountLabel.setText(String.valueOf(plateformes.size()));
        actifsCountLabel.setText(String.valueOf(actifs));
    }

    private VBox buildGameCard(Jeu jeu) {
        VBox card = new VBox(0);
        card.setPrefWidth(CARD_W);
        card.setMaxWidth(CARD_W);
        card.setStyle(CARD_STYLE);

        StackPane imgStack = new StackPane();
        imgStack.setPrefSize(CARD_W, IMG_H);
        imgStack.setMaxSize(CARD_W, IMG_H);

        Rectangle imgClip = new Rectangle(CARD_W, IMG_H);
        imgClip.setArcWidth(22);
        imgClip.setArcHeight(22);
        imgStack.setClip(imgClip);

        Image cover = resolveGameCover(jeu);
        boolean hasCover = cover != null && !cover.isError();
        if (!hasCover) {
            Rectangle grad = new Rectangle(CARD_W, IMG_H);
            grad.setArcWidth(22);
            grad.setArcHeight(22);
            grad.setFill(new LinearGradient(0, 0, 1, 1, true, null,
                    new Stop(0, Color.web("#5f56ff")),
                    new Stop(0.5, Color.web("#7c3aed")),
                    new Stop(1, Color.web("#3b1022"))));
            imgStack.getChildren().add(grad);
        } else {
            ImageView iv = new ImageView(cover);
            iv.setFitWidth(CARD_W);
            iv.setFitHeight(IMG_H);
            iv.setPreserveRatio(false);
            imgStack.getChildren().add(iv);
        }

        String ribbonText = ribbonLabelFor(jeu);
        Label ribbon = new Label(ribbonText);
        ribbon.setWrapText(true);
        ribbon.setMaxWidth(140);
        ribbon.setStyle(
                "-fx-background-color: #f2d061;"
                        + "-fx-text-fill: #0d1b3e;"
                        + "-fx-font-size: 9px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 5 10;"
                        + "-fx-background-radius: 2;"
        );
        ribbon.setRotate(-12);
        StackPane.setAlignment(ribbon, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(ribbon, new Insets(0, 8, 10, 0));
        imgStack.getChildren().add(ribbon);

        Label title = new Label(jeu.getNom() != null ? jeu.getNom() : "—");
        title.setWrapText(true);
        title.setMaxWidth(CARD_W - 24);
        title.setStyle(
                "-fx-text-fill: white;"
                        + "-fx-font-size: 14px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 14 14 12 14;"
        );
        VBox titleBox = new VBox(title);
        titleBox.setStyle("-fx-background-color: #0a1228;");

        String genre = emptyAsDash(jeu.getGenre());
        String plateforme = emptyAsDash(jeu.getPlateforme());
        String statut = emptyAsDash(jeu.getStatut());
        String desc = jeu.getDescription() != null ? jeu.getDescription() : "—";
        if (desc.length() > 48) {
            desc = desc.substring(0, 45) + "…";
        }

        VBox details = new VBox(0);
        details.getChildren().addAll(
                genrePlateformeRow(genre, plateforme),
                infoRow("Statut", statut, statutStyle(statut)),
                infoRow("Description", desc, "-fx-text-fill: #94a3b8; -fx-font-size: 11px;"),
                footerRow(jeu)
        );

        card.getChildren().addAll(imgStack, titleBox, details);
        configureCardInteraction(card, jeu);
        return card;
    }

    private void configureCardInteraction(VBox card, Jeu jeu) {
        card.setCursor(Cursor.HAND);
        card.setOnMouseEntered(e -> card.setStyle(CARD_STYLE_HOVER));
        card.setOnMouseExited(e -> card.setStyle(CARD_STYLE));
        card.setOnMouseClicked(e -> showJeuDetails(jeu));
    }

    private void showJeuDetails(Jeu jeu) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Détails du jeu");
        info.setHeaderText(jeu.getNom() == null ? "Jeu" : jeu.getNom());
        info.setContentText(
                "Genre: " + emptyAsDash(jeu.getGenre()) + "\n"
                        + "Plateforme: " + emptyAsDash(jeu.getPlateforme()) + "\n"
                        + "Statut: " + emptyAsDash(jeu.getStatut()) + "\n"
                        + "Description: " + emptyAsDash(jeu.getDescription())
        );
        info.showAndWait();
    }

    private static String ribbonLabelFor(Jeu jeu) {
        if (jeu.getGenre() != null && !jeu.getGenre().isBlank()) {
            String g = jeu.getGenre().toUpperCase();
            return g.length() > 22 ? g.substring(0, 19) + "…" : g;
        }
        return "CATALOGUE";
    }

    private static String emptyAsDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private HBox genrePlateformeRow(String genre, String plateforme) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 0 0 1 0;");

        Label left = new Label("🎮  " + genre);
        left.setWrapText(true);
        left.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");
        left.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label right = new Label(plateforme);
        right.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 12px; -fx-font-weight: bold;");
        row.getChildren().addAll(left, right);
        return row;
    }

    private HBox infoRow(String leftText, String rightText, String rightExtraStyle) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 0 0 1 0;");

        Label left = new Label(leftText);
        left.setWrapText(true);
        left.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px;");
        left.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(left, Priority.ALWAYS);

        if (rightText.isEmpty()) {
            row.getChildren().add(left);
            return row;
        }

        Label right = new Label(rightText);
        right.setWrapText(true);
        right.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;" + (rightExtraStyle.isEmpty() ? " -fx-text-fill: #a78bfa;" : " " + rightExtraStyle));
        row.getChildren().addAll(left, right);
        return row;
    }

    private static String statutStyle(String statut) {
        String s = statut.toLowerCase();
        if (s.contains("fin") || s.contains("termin")) {
            return "-fx-text-fill: #f59e0b;";
        }
        if (s.contains("ferm") || s.contains("indis")) {
            return "-fx-text-fill: #f87171;";
        }
        if (s.contains("actif") || s.contains("dispo") || s.contains("ouvert")) {
            return "-fx-text-fill: #4ade80;";
        }
        return "-fx-text-fill: #a78bfa;";
    }

    private HBox footerRow(Jeu jeu) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 14, 14, 14));
        row.setSpacing(10);

        if (!readOnly) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button editBtn = new Button("Modifier");
            editBtn.setStyle(
                    "-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 11px;"
                            + "-fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 6;"
            );
            editBtn.setOnAction(e -> openEditWindow(jeu));

            Button deleteBtn = new Button("Supprimer");
            deleteBtn.setStyle(
                    "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 11px;"
                            + "-fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 6;"
            );
            deleteBtn.setOnAction(e -> confirmDelete(jeu));

            row.getChildren().addAll(spacer, editBtn, deleteBtn);
        }

        return row;
    }

    @FXML
    private void handleNewJeu() {
        if (readOnly) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterJeu.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un jeu");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadJeux();
        } catch (IOException e) {
            showError("Erreur d'ouverture du formulaire : " + e.getMessage());
        }
    }

    private void openEditWindow(Jeu j) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterJeu.fxml"));
            Parent root = loader.load();
            AjouterJeuController controller = loader.getController();
            controller.setJeuToEdit(j);
            Stage stage = new Stage();
            stage.setTitle("Modifier le jeu");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadJeux();
        } catch (IOException e) {
            showError("Erreur d'ouverture du formulaire : " + e.getMessage());
        }
    }

    private void confirmDelete(Jeu j) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer le jeu « " + j.getNom() + " » ?");
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                try {
                    serviceJeu.supprimer(j.getId());
                    messageLabel.setStyle("-fx-text-fill: #4ade80;");
                    messageLabel.setText("Jeu supprimé.");
                    loadJeux();
                } catch (SQLException ex) {
                    showError("Suppression impossible : " + ex.getMessage());
                }
            }
        });
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    private void filterAll() {
        setActiveFilterButton(allFilterBtn);
        showJeux(allJeux);
    }

    @FXML
    private void filterActive() {
        setActiveFilterButton(activeFilterBtn);
        showJeux(filterBy(j -> {
            String s = j.getStatut() == null ? "" : j.getStatut().toLowerCase();
            return s.contains("actif") || s.contains("dispo") || s.contains("ouvert");
        }));
    }

    @FXML
    private void filterFps() {
        setActiveFilterButton(fpsFilterBtn);
        showJeux(filterBy(j -> containsIgnoreCase(j.getGenre(), "fps")));
    }

    @FXML
    private void filterBattleRoyale() {
        setActiveFilterButton(brFilterBtn);
        showJeux(filterBy(j -> containsIgnoreCase(j.getGenre(), "battle")));
    }

    @FXML
    private void filterSport() {
        setActiveFilterButton(sportFilterBtn);
        showJeux(filterBy(j -> containsIgnoreCase(j.getGenre(), "sport")));
    }

    private List<Jeu> filterBy(Predicate<Jeu> predicate) {
        List<Jeu> out = new ArrayList<>();
        for (Jeu j : allJeux) {
            if (predicate.test(j)) {
                out.add(j);
            }
        }
        return out;
    }

    private static boolean containsIgnoreCase(String value, String expected) {
        return value != null && value.toLowerCase().contains(expected.toLowerCase());
    }

    private Image resolveGameCover(Jeu jeu) {
        String nom = jeu.getNom() == null ? "" : jeu.getNom().trim();
        if (nom.isEmpty()) {
            return coverPlaceholder;
        }

        String normalized = normalizeFileName(nom);
        if (imageCache.containsKey(normalized)) {
            return imageCache.get(normalized);
        }

        String[] exts = {".png", ".jpg", ".jpeg", ".webp"};
        String[] names = {
                nom,
                nom.toLowerCase(Locale.ROOT),
                normalized
        };

        for (String base : names) {
            for (String ext : exts) {
                URL imageUrl = getClass().getResource("/images/jeux/" + base + ext);
                if (imageUrl != null) {
                    Image image = new Image(imageUrl.toExternalForm(), CARD_W, IMG_H, false, true);
                    imageCache.put(normalized, image);
                    return image;
                }
            }
        }

        imageCache.put(normalized, coverPlaceholder);
        return coverPlaceholder;
    }

    private String normalizeFileName(String input) {
        String cleaned = input.toLowerCase(Locale.ROOT).trim();
        cleaned = cleaned.replace(':', ' ');
        cleaned = cleaned.replaceAll("[^a-z0-9\\s-]", "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    private void setActiveFilterButton(Button activeButton) {
        String idle = "-fx-background-color: #26365f; -fx-text-fill: #e2e8f0; -fx-background-radius: 8; -fx-cursor: hand;";
        String active = "-fx-background-color: #7c3aed; -fx-text-fill: #ffffff; -fx-border-color: #a78bfa; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        allFilterBtn.setStyle(idle);
        activeFilterBtn.setStyle(idle);
        fpsFilterBtn.setStyle(idle);
        brFilterBtn.setStyle(idle);
        sportFilterBtn.setStyle(idle);
        activeButton.setStyle(active);
    }
}
