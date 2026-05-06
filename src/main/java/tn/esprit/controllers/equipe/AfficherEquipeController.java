package tn.esprit.controllers.equipe;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.OwnerDashboardStats;
import tn.esprit.entities.TeamInvitation;

import tn.esprit.services.ServiceEquipe;
import tn.esprit.utils.SessionManager;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class AfficherEquipeController implements Initializable {

    @FXML
    private FlowPane cardsContainer;
    @FXML
    private Label totalTeamsLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private VBox invitationsContainer;

    private final ServiceEquipe serviceEquipe = new ServiceEquipe();
    private final List<Equipe> allEquipes = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupSearchAndSort();
        loadEquipes();
        loadInvitations();
    }

    private void setupSearchAndSort() {
        sortCombo.getItems().addAll("Nom A-Z", "Nom Z-A", "Max members croissant", "Max members decroissant");
        sortCombo.getSelectionModel().select("Nom A-Z");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderCards());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> renderCards());
    }

    private void loadEquipes() {
        try {
            List<Equipe> equipes = serviceEquipe.getAll();
            allEquipes.clear();
            allEquipes.addAll(equipes);
            totalTeamsLabel.setText(String.valueOf(equipes.size()));
            renderCards();
            messageLabel.setText("");
        } catch (SQLException e) {
            messageLabel.setText("Erreur chargement equipes : " + e.getMessage());
        }
    }

    private void renderCards() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        List<Equipe> filtered = new ArrayList<>();
        for (Equipe equipe : allEquipes) {
            if (keyword.isEmpty()
                    || safeLower(equipe.getNom()).contains(keyword)
                    || safeLower(equipe.getLogo()).contains(keyword)
                    || String.valueOf(equipe.getId()).contains(keyword)
                    || String.valueOf(equipe.getMaxMembers()).contains(keyword)) {
                filtered.add(equipe);
            }
        }

        filtered.sort(getSortComparator(sortCombo.getValue()));

        cardsContainer.getChildren().clear();
        int rank = 1;
        for (Equipe equipe : filtered) {
            cardsContainer.getChildren().add(buildCard(equipe, rank++));
        }

        if (filtered.isEmpty()) {
            messageLabel.setText("Aucune equipe trouvee.");
        } else {
            messageLabel.setText("");
        }
    }

    private Comparator<Equipe> getSortComparator(String sortValue) {
        if ("Nom Z-A".equals(sortValue)) {
            return Comparator.comparing((Equipe e) -> safeLower(e.getNom())).reversed();
        }
        if ("Max members croissant".equals(sortValue)) {
            return Comparator.comparingInt(Equipe::getMaxMembers);
        }
        if ("Max members decroissant".equals(sortValue)) {
            return Comparator.comparingInt(Equipe::getMaxMembers).reversed();
        }
        return Comparator.comparing(e -> safeLower(e.getNom()));
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    // ═══════════════════════════════════════════
    //  INVITATION NOTIFICATIONS
    // ═══════════════════════════════════════════

    private void loadInvitations() {
        invitationsContainer.getChildren().clear();
        try {
            List<TeamInvitation> invitations = serviceEquipe.getInvitationsForCurrentUser();
            if (invitations.isEmpty()) return;

            // Header label
            Label header = new Label("✉ Invitations reçues (" + invitations.size() + ")");
            header.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 14px; -fx-font-weight: 700;");
            invitationsContainer.getChildren().add(header);

            for (TeamInvitation inv : invitations) {
                invitationsContainer.getChildren().add(buildInvitationCard(inv));
            }
        } catch (SQLException e) {
            // Silent: invitations are optional UI
            System.out.println("Invitations load error: " + e.getMessage());
        }
    }

    private HBox buildInvitationCard(TeamInvitation inv) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12; -fx-padding: 12 18;"
            + "-fx-border-color: rgba(124,58,237,0.25); -fx-border-radius: 12; -fx-border-width: 1;");

        Label icon = new Label("✉");
        icon.setStyle("-fx-font-size: 18px;");

        VBox info = new VBox(2);
        Label title = new Label("Invitation de " + inv.getOwnerNom() + " pour l'équipe " + inv.getEquipeNom());
        title.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: 700;");
        Label date = new Label(inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : "");
        date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        info.getChildren().addAll(title, date);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button acceptBtn = new Button("✓ Accepter");
        acceptBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: 700;"
            + "-fx-background-radius: 10; -fx-pref-height: 32; -fx-pref-width: 110; -fx-cursor: hand; -fx-font-size: 12px;");
        acceptBtn.setOnAction(e -> handleAcceptInvitation(inv));

        Button refuseBtn = new Button("✕ Refuser");
        refuseBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 700;"
            + "-fx-background-radius: 10; -fx-pref-height: 32; -fx-pref-width: 110; -fx-cursor: hand; -fx-font-size: 12px;");
        refuseBtn.setOnAction(e -> handleRefuseInvitation(inv));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(icon, info, spacer, acceptBtn, refuseBtn);
        return card;
    }

    private void handleAcceptInvitation(TeamInvitation inv) {
        try {
            serviceEquipe.respondToInvitation(inv.getId(), true);
            messageLabel.setStyle("-fx-text-fill: #a78bfa;");
            messageLabel.setText("Invitation acceptée ! Vous avez rejoint l'équipe " + inv.getEquipeNom() + ".");
            loadEquipes();
            loadInvitations();
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private void handleRefuseInvitation(TeamInvitation inv) {
        try {
            serviceEquipe.respondToInvitation(inv.getId(), false);
            messageLabel.setStyle("-fx-text-fill: #a78bfa;");
            messageLabel.setText("Invitation refusée.");
            loadInvitations();
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #f87171;");
            messageLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private VBox buildCard(Equipe equipe, int rank) {
        VBox card = new VBox(10);
        card.setPrefWidth(290);
        card.setPrefHeight(320);
        card.setStyle("-fx-padding: 18 16 14 16;"
            + "-fx-background-color: #111b3e;"
            + "-fx-background-radius: 16;"
            + "-fx-border-color: rgba(124,58,237,0.18);"
            + "-fx-border-radius: 16;"
            + "-fx-border-width: 1;"
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 12, 0, 0, 4);");

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(
            "rgba(124,58,237,0.18)", "rgba(124,58,237,0.45)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
            "rgba(124,58,237,0.45)", "rgba(124,58,237,0.18)")));

        Label rankLabel = new Label("#" + rank);
        rankLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: rgba(255,255,255,0.14);");

        StackPane avatarPane = createAvatar(equipe);

        Label nomLabel = new Label(equipe.getNom());
        nomLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #f1f5f9;");

        Label subtitle = new Label("ESPORT TEAM");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: 600;");

        VBox header = new VBox(rankLabel);
        header.setAlignment(Pos.TOP_LEFT);

        VBox body = new VBox(8, avatarPane, nomLabel, subtitle);
        body.setAlignment(Pos.CENTER);

        // ── QR Code Button ──
        Button qrBtn = new Button("📱 QR");
        qrBtn.setStyle("-fx-background-color: rgba(124,58,237,0.25); -fx-text-fill: #a78bfa; -fx-font-weight: 700;"
            + "-fx-background-radius: 8; -fx-pref-height: 28; -fx-pref-width: 70; -fx-cursor: hand; -fx-font-size: 11px;");
        qrBtn.setOnMouseEntered(e -> qrBtn.setStyle(qrBtn.getStyle().replace("rgba(124,58,237,0.25)", "rgba(124,58,237,0.45)")));
        qrBtn.setOnMouseExited(e -> qrBtn.setStyle(qrBtn.getStyle().replace("rgba(124,58,237,0.45)", "rgba(124,58,237,0.25)")));
        qrBtn.setOnAction(e -> showQrCodePopup(equipe));

        HBox qrRow = new HBox(qrBtn);
        qrRow.setAlignment(Pos.CENTER_RIGHT);

        VBox actions = buildActions(equipe, card);
        card.getChildren().addAll(header, body, qrRow, actions);
        return card;
    }

    private VBox buildActions(Equipe equipe, VBox card) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);

        if (SessionManager.getCurrentUser() == null || SessionManager.getCurrentUser().getId() <= 0) {
            Label guestLabel = new Label("Connectez-vous pour interagir");
            guestLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            box.getChildren().add(guestLabel);
            return box;
        }

        try {
            boolean isOwner = serviceEquipe.isCurrentUserOwnerOfEquipe(equipe.getId());
            if (isOwner) {
                Label ownerLabel = new Label("OWNER - Cliquez la carte pour ouvrir le dashboard");
                ownerLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px; -fx-font-weight: 700;");
                box.getChildren().add(ownerLabel);

                card.setOnMouseClicked(evt -> openOwnerDashboard());
                card.setStyle(card.getStyle() + "-fx-cursor: hand;");
                return box;
            }

            if (serviceEquipe.isCurrentUserMemberOfEquipe(equipe.getId())) {
                Label memberLabel = new Label("Vous êtes membre de cette équipe");
                memberLabel.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 12px; -fx-font-weight: 700;");
                Button leaveBtn = new Button("Quitter l'équipe");
                leaveBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 700; -fx-pref-width: 200; -fx-pref-height: 36; -fx-background-radius: 10; -fx-cursor: hand;");
                leaveBtn.setOnAction(evt -> {
                    evt.consume();
                    handleLeaveEquipe(equipe.getId());
                });
                box.getChildren().addAll(memberLabel, leaveBtn);
                return box;
            }

            boolean pending = serviceEquipe.hasPendingRequestForCurrentUser(equipe.getId());
            if (pending) {
                Button cancelBtn = new Button("Supprimer invite");
                cancelBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 700; -fx-pref-width: 200; -fx-pref-height: 36; -fx-background-radius: 10; -fx-cursor: hand;");
                cancelBtn.setOnAction(evt -> {
                    evt.consume();
                    handleCancelInvite(equipe.getId());
                });
                box.getChildren().add(cancelBtn);
            } else {
                Button joinBtn = new Button("Demander a joindre");
                joinBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: 700; -fx-pref-width: 200; -fx-pref-height: 36; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 13px;");
                joinBtn.setOnAction(evt -> {
                    evt.consume();
                    handleJoinRequest(equipe.getId());
                });
                box.getChildren().add(joinBtn);
            }
        } catch (SQLException e) {
            Label errLabel = new Label("Etat indisponible");
            errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
            box.getChildren().add(errLabel);
        }

        return box;
    }

    // ═══════════════════════════════════════════
    //  QR CODE — REST API CALL
    // ═══════════════════════════════════════════

    private void showQrCodePopup(Equipe equipe) {
        // Fetch team details
        int memberCount = 0;
        String ownerName = "—";
        int wins = 0;
        int totalMatchs = 0;
        try {
            memberCount = serviceEquipe.countMembers(equipe.getId());
            ownerName = serviceEquipe.getOwnerNameByEquipeId(equipe.getId());
            OwnerDashboardStats stats = serviceEquipe.getOwnerDashboardStats(equipe.getId());
            wins = stats.getWins();
            totalMatchs = stats.getTotalFinishedMatches();
        } catch (SQLException ignored) {}

        // ── Appel REST API externe : qrserver.com ──
        String qrData = equipe.getNom()
                + "\nMembres: " + memberCount + "/" + equipe.getMaxMembers()
                + "\nVictoires: " + wins + "/" + totalMatchs + " matchs";
        String encodedData;
        try {
            encodedData = java.net.URLEncoder.encode(qrData, "UTF-8");
        } catch (Exception ex) {
            encodedData = qrData.replace(" ", "+");
        }
        String apiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&format=png&data=" + encodedData;

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.setTitle("QR Code — " + equipe.getNom());

        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f172a; -fx-padding: 28; -fx-background-radius: 18;"
            + "-fx-border-color: rgba(124,58,237,0.4); -fx-border-radius: 18; -fx-border-width: 2;");

        // Title
        Label title = new Label("📱 QR Code");
        title.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 20px; -fx-font-weight: 800;");

        Label teamName = new Label(equipe.getNom());
        teamName.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 18px; -fx-font-weight: 700;");

        Label subtitle = new Label("ESPORT TEAM");
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-weight: 600;");


        // QR Code image from API
        ImageView qrImageView = new ImageView();
        qrImageView.setFitWidth(200);
        qrImageView.setFitHeight(200);
        qrImageView.setPreserveRatio(true);

        Label loadingLabel = new Label("Chargement...");
        loadingLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        // Load image from REST API in background
        new Thread(() -> {
            try {
                Image qrImage = new Image(apiUrl, true);
                javafx.application.Platform.runLater(() -> {
                    qrImageView.setImage(qrImage);
                    loadingLabel.setText("Scannez pour découvrir l'équipe");
                    loadingLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    loadingLabel.setText("Erreur API : " + ex.getMessage());
                    loadingLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
                });
            }
        }).start();

        // API info label
        Label apiLabel = new Label("🔗 REST API: api.qrserver.com");
        apiLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");

        // Close button
        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: 700;"
            + "-fx-background-radius: 10; -fx-pref-height: 36; -fx-pref-width: 140; -fx-cursor: hand; -fx-font-size: 13px;");
        closeBtn.setOnAction(e -> popup.close());

        root.getChildren().addAll(title, teamName, subtitle, qrImageView, loadingLabel, apiLabel, closeBtn);

        Scene scene = new Scene(root, 340, 460);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene);
        popup.showAndWait();
    }

    private void handleJoinRequest(int equipeId) {
        try {
            serviceEquipe.createJoinRequest(equipeId);
            messageLabel.setText("Demande envoyee en statut pending.");
            messageLabel.setStyle("-fx-text-fill: #a78bfa;");
            renderCards();
        } catch (SQLException e) {
            messageLabel.setText("Demande impossible : " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: #f87171;");
        }
    }

    private void handleCancelInvite(int equipeId) {
        try {
            serviceEquipe.cancelPendingJoinRequestForCurrentUser(equipeId);
            messageLabel.setText("Invite supprimee (pending annule).");
            messageLabel.setStyle("-fx-text-fill: #a78bfa;");
            renderCards();
        } catch (SQLException e) {
            messageLabel.setText("Suppression invite impossible : " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: #f87171;");
        }
    }

    private void handleLeaveEquipe(int equipeId) {
        try {
            serviceEquipe.leaveEquipe(equipeId);
            messageLabel.setText("Vous avez quitte l'equipe.");
            messageLabel.setStyle("-fx-text-fill: #a78bfa;");
            renderCards();
        } catch (SQLException e) {
            messageLabel.setText("Impossible de quitter l'equipe : " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: #f87171;");
        }
    }

    private void openOwnerDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/equipe/ownerDashboardView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Owner Dashboard");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            messageLabel.setText("Ouverture Owner Dashboard impossible : " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: #f87171;");
        }
    }

    @FXML
    private void handleAddEquipe() {
        openAddEquipeForm();
    }

    private void openAddEquipeForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/equipe/ajouterEquipe.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter une equipe");
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
            loadEquipes();
        } catch (Exception e) {
            messageLabel.setText("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    private StackPane createAvatar(Equipe equipe) {
        Circle circle = new Circle(40);
        circle.setFill(Color.web("#172554"));
        circle.setStroke(Color.web("#7c3aed"));
        circle.setStrokeWidth(2);

        StackPane avatar = new StackPane(circle);
        avatar.setAlignment(Pos.CENTER);

        String logo = equipe.getLogo();
        if (logo != null && !logo.isBlank()) {
            String imageUrl = null;

            // Web URL (API teams)
            if (logo.startsWith("http://") || logo.startsWith("https://")) {
                imageUrl = logo;
            } else {
                // Local file path
                File file = new File(logo);
                if (file.exists()) {
                    imageUrl = file.toURI().toString();
                }
            }

            if (imageUrl != null) {
                try {
                    Image image = new Image(imageUrl, 80, 80, true, true, true);
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(80);
                    imageView.setFitHeight(80);
                    imageView.setClip(new Circle(40, 40, 40));
                    avatar.getChildren().add(imageView);
                    return avatar;
                } catch (Exception e) {
                    // Fallback to initials if image loading fails
                }
            }
        }

        Label initials = new Label(getInitials(equipe.getNom()));
        initials.setStyle("-fx-font-size: 22px; -fx-text-fill: #e5e7eb; -fx-font-weight: 700;");
        avatar.getChildren().add(initials);
        return avatar;
    }

    private String getInitials(String nom) {
        if (nom == null || nom.isBlank()) {
            return "?";
        }
        String trimmed = nom.trim();
        if (trimmed.length() == 1) {
            return trimmed.toUpperCase();
        }
        return trimmed.substring(0, 2).toLowerCase();
    }

}
