package tn.esprit.controllers.equipe;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import tn.esprit.entities.EquipeMemberView;
import tn.esprit.entities.OwnerDashboardStats;
import tn.esprit.services.ServiceEquipe;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class OwnerDashboardViewController implements Initializable {

    @FXML
    private Label pendingRequestsLabel;
    @FXML
    private Label finishedMatchesLabel;
    @FXML
    private Label membersMaxLabel;
    @FXML
    private Label resultsLabel;
    @FXML
    private Label winRateLabel;

    @FXML
    private TextField searchField;
    @FXML
    private FlowPane memberCardsContainer;

    @FXML
    private Label messageLabel;

    private final ServiceEquipe serviceEquipe = new ServiceEquipe();
    private List<EquipeMemberView> allMembers = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupSearch();
        loadStats();
        loadTeamMembers();
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> renderCards());
    }

    @FXML
    private void handleRefresh() {
        loadStats();
        loadTeamMembers();
        messageLabel.setStyle("-fx-text-fill: #a78bfa;");
        messageLabel.setText("Liste mise à jour.");
    }

    private void renderCards() {
        memberCardsContainer.getChildren().clear();
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        for (EquipeMemberView member : allMembers) {
            if (matchesQuery(member, query)) {
                memberCardsContainer.getChildren().add(createMemberCard(member));
            }
        }
    }

    private boolean matchesQuery(EquipeMemberView member, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(member.getJoueurNom(), query)
                || containsIgnoreCase(member.getJoueurEmail(), query)
                || containsIgnoreCase(member.getEquipeNom(), query)
                || containsIgnoreCase(member.getStatus(), query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private VBox createMemberCard(EquipeMemberView member) {
        VBox card = new VBox(10);
        String baseStyle = "-fx-background-color: #111b3e;"
            + "-fx-background-radius: 16;"
            + "-fx-padding: 18 16 14 16;"
            + "-fx-border-color: rgba(124,58,237,0.18);"
            + "-fx-border-radius: 16;"
            + "-fx-border-width: 1;"
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 4);";
        card.setStyle(baseStyle);
        card.setPrefWidth(270);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(baseStyle.replace(
            "rgba(124,58,237,0.18)", "rgba(124,58,237,0.45)")));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        // ── Avatar + Name Row ──
        String initials = getInitials(member.getJoueurNom());
        Circle avatarBg = new Circle(22);
        avatarBg.setFill(Color.web("#7c3aed"));
        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 800;");
        StackPane avatar = new StackPane(avatarBg, initialsLabel);

        VBox nameBlock = new VBox(2);
        Label playerLabel = new Label(member.getJoueurNom());
        playerLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 16px; -fx-font-weight: 800;");
        Label emailLabel = new Label(member.getJoueurEmail());
        emailLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        nameBlock.getChildren().addAll(playerLabel, emailLabel);

        HBox topRow = new HBox(12, avatar, nameBlock);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // ── Status Badge ──
        Label statusLabel = new Label(member.getStatus());
        String statusStyle;
        if ("Member".equalsIgnoreCase(member.getStatus())) {
            statusStyle = "-fx-background-color: rgba(124,58,237,0.18); -fx-text-fill: #c4b5fd;";
        } else if ("Pending".equalsIgnoreCase(member.getStatus())) {
            statusStyle = "-fx-background-color: rgba(245,158,11,0.18); -fx-text-fill: #fcd34d;";
        } else if ("Invited".equalsIgnoreCase(member.getStatus())) {
            statusStyle = "-fx-background-color: rgba(14,165,233,0.18); -fx-text-fill: #7dd3fc;";
        } else {
            statusStyle = "-fx-background-color: rgba(100,116,139,0.18); -fx-text-fill: #94a3b8;";
        }
        statusLabel.setStyle(statusStyle + "-fx-padding: 4 12; -fx-background-radius: 8; -fx-font-size: 11px; -fx-font-weight: 700;");

        HBox badgeRow = new HBox(10, statusLabel);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        // ── Spacer ──
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ── Action Buttons ──
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        if ("Pending".equalsIgnoreCase(member.getStatus())) {
            Button acceptBtn = new Button("✓ Accepter");
            acceptBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: 700;"
                + "-fx-background-radius: 10; -fx-pref-height: 34; -fx-pref-width: 110; -fx-cursor: hand; -fx-font-size: 12px;");
            acceptBtn.setOnAction(e -> handleAcceptRequest(member));
            Button rejectBtn = new Button("✕ Rejeter");
            rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 700;"
                + "-fx-background-radius: 10; -fx-pref-height: 34; -fx-pref-width: 110; -fx-cursor: hand; -fx-font-size: 12px;");
            rejectBtn.setOnAction(e -> handleRejectRequest(member));
            actionBox.getChildren().addAll(acceptBtn, rejectBtn);
        } else if ("Member".equalsIgnoreCase(member.getStatus())) {
            Button removeBtn = new Button("Supprimer");
            removeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 700;"
                + "-fx-background-radius: 10; -fx-pref-height: 34; -fx-pref-width: 120; -fx-cursor: hand; -fx-font-size: 12px;");
            removeBtn.setOnAction(e -> handleRemoveMember(member));
            actionBox.getChildren().add(removeBtn);
        } else if ("Invited".equalsIgnoreCase(member.getStatus())) {
            Label invitedLabel = new Label("✉ Invitation envoyée");
            invitedLabel.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 12px; -fx-font-weight: 700;");
            actionBox.getChildren().add(invitedLabel);
        } else {
            Button inviteBtn = new Button("✉ Inviter");
            inviteBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: 700;"
                + "-fx-background-radius: 10; -fx-pref-height: 34; -fx-pref-width: 120; -fx-cursor: hand; -fx-font-size: 12px;");
            inviteBtn.setOnAction(e -> handleInviteMember(member));
            actionBox.getChildren().add(inviteBtn);
        }

        card.getChildren().addAll(topRow, badgeRow, spacer, actionBox);
        return card;
    }

    private String getInitials(String nom) {
        if (nom == null || nom.isBlank()) return "?";
        String[] parts = nom.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return nom.trim().substring(0, Math.min(2, nom.trim().length())).toUpperCase();
    }

    private void handleAcceptRequest(EquipeMemberView member) {
        if (member.getRequestId() == null) {
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            messageLabel.setText("Aucune demande à accepter.");
            return;
        }
        try {
            serviceEquipe.processJoinRequest(member.getRequestId(), true, null);
            loadStats();
            loadTeamMembers();
            messageLabel.setStyle("-fx-text-fill: #a78bfa;");
            messageLabel.setText("Demande acceptée.");
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            messageLabel.setText("Acceptation impossible : " + e.getMessage());
        }
    }

    private void handleRejectRequest(EquipeMemberView member) {
        if (member.getRequestId() == null) {
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            messageLabel.setText("Aucune demande à rejeter.");
            return;
        }
        try {
            serviceEquipe.processJoinRequest(member.getRequestId(), false, null);
            loadStats();
            loadTeamMembers();
            messageLabel.setStyle("-fx-text-fill: #a78bfa;");
            messageLabel.setText("Demande rejetée.");
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            messageLabel.setText("Rejet impossible : " + e.getMessage());
        }
    }

    private void handleInviteMember(EquipeMemberView member) {
        try {
            List<tn.esprit.entities.Equipe> owned = serviceEquipe.getOwnedEquipes(
                    tn.esprit.utils.SessionManager.getCurrentUser().getId());
            if (owned.isEmpty()) {
                messageLabel.setStyle("-fx-text-fill: #ef4444;");
                messageLabel.setText("Aucune équipe trouvée.");
                return;
            }
            serviceEquipe.createInvitation(owned.get(0).getId(), member.getJoueurId());
            loadStats();
            loadTeamMembers();
            messageLabel.setStyle("-fx-text-fill: #a78bfa;");
            messageLabel.setText("Invitation envoyée à " + member.getJoueurNom() + " (email de notification envoyé).");
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            messageLabel.setText("Invitation impossible : " + e.getMessage());
        }
    }

    private void handleRemoveMember(EquipeMemberView member) {
        try {
            serviceEquipe.removeMemberFromEquipe(member.getEquipeId(), member.getJoueurId());
            loadStats();
            loadTeamMembers();
            messageLabel.setStyle("-fx-text-fill: #a78bfa;");
            messageLabel.setText("Membre supprimé.");
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            messageLabel.setText("Suppression impossible : " + e.getMessage());
        }
    }

    private void loadTeamMembers() {
        try {
            allMembers = serviceEquipe.getOwnerTeamPlayersAndRequests();
            renderCards();
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            messageLabel.setText("Impossible de charger la liste : " + e.getMessage());
        }
    }

    private void loadStats() {
        try {
            OwnerDashboardStats stats = serviceEquipe.getOwnerDashboardStatsForCurrentOwner();
            pendingRequestsLabel.setText(String.valueOf(stats.getPendingRequestsCount()));
            finishedMatchesLabel.setText(String.valueOf(stats.getTotalFinishedMatches()));
            membersMaxLabel.setText(stats.getTotalMembers() + " / " + stats.getTotalMaxMembers());
            resultsLabel.setText("V: " + stats.getWins() + "   N: " + stats.getDraws() + "   P: " + stats.getLosses());
            winRateLabel.setText(stats.getWinRate() + "%");

            messageLabel.setText("");
        } catch (SQLException e) {
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            messageLabel.setText("Erreur dashboard owner: " + e.getMessage());
        }
    }
}
