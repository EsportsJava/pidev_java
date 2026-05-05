package tn.esprit.services;

import tn.esprit.entities.EquipeJoinRequest;
import tn.esprit.entities.EquipeStanding;
import tn.esprit.entities.OwnerDashboardStats;
import tn.esprit.entities.TeamInvitation;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.User;
import tn.esprit.utils.MyDatabase;
import tn.esprit.utils.SessionManager;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServiceEquipe implements IService<Equipe> {
    private Connection conn;

    public ServiceEquipe() {
        conn = MyDatabase.getInstance().getConnection();
        try {
            ensureAdvancedTables();
        } catch (SQLException e) {
            throw new RuntimeException("Initialisation des tables equipe avancees impossible: " + e.getMessage(), e);
        }
    }

    @Override
    public void ajouter(Equipe equipe) throws SQLException {
        String sql = "INSERT INTO equipe(nom, max_members, logo, owner_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, equipe.getNom());
            ps.setInt(2, equipe.getMaxMembers());
            ps.setString(3, equipe.getLogo());
            ps.setInt(4, resolveOwnerId());
            ps.executeUpdate();
        }
    }

    private int resolveOwnerId() throws SQLException {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && currentUser.getId() > 0) {
            return currentUser.getId();
        }

        String fallbackSql = "SELECT id FROM `user` ORDER BY id ASC LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(fallbackSql)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        throw new SQLException("Aucun utilisateur trouve pour owner_id. Creez un utilisateur puis reessayez.");
    }

    @Override
    public void modifier(Equipe equipe) throws SQLException {
        String sql = "UPDATE equipe SET nom=?, max_members=?, logo=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, equipe.getNom());
            ps.setInt(2, equipe.getMaxMembers());
            ps.setString(3, equipe.getLogo());
            ps.setInt(4, equipe.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM equipe WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Equipe> getAll() throws SQLException {
        String sql = "SELECT * FROM equipe";
        List<Equipe> equipes = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Equipe equipe = new Equipe(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("max_members"),
                    rs.getString("logo")
                );
                equipes.add(equipe);
            }
        }

        return equipes;
    }

    public int createJoinRequest(int equipeId) throws SQLException {
        int joueurId = getCurrentUserIdOrThrow();

        EquipeMeta meta = getEquipeMeta(equipeId);
        if (meta == null) {
            throw new SQLException("Equipe introuvable.");
        }
        if (meta.ownerId == joueurId) {
            throw new SQLException("Le owner ne peut pas envoyer une demande d'adhesion a sa propre equipe.");
        }
        if (isUserMemberOfTeam(equipeId, joueurId)) {
            throw new SQLException("Ce joueur est deja membre de cette equipe.");
        }
        if (countMembers(equipeId) >= meta.maxMembers) {
            throw new SQLException("Demande refusee: l'equipe est deja pleine.");
        }
        if (hasPendingRequest(equipeId, joueurId)) {
            throw new SQLException("Une demande pending existe deja pour ce joueur et cette equipe.");
        }

        String sql = "INSERT INTO join_request(equipe_id, user_id, status, created_at, created_by_id) VALUES (?, ?, 'pending', NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, joueurId);
            ps.setInt(3, joueurId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Creation de la demande impossible.");
    }

    public boolean isCurrentUserOwnerOfEquipe(int equipeId) throws SQLException {
        int userId = getCurrentUserIdOrThrow();
        EquipeMeta meta = getEquipeMeta(equipeId);
        return meta != null && meta.ownerId == userId;
    }

    public boolean isCurrentUserMemberOfEquipe(int equipeId) throws SQLException {
        int userId = getCurrentUserIdOrThrow();
        return isUserMemberOfTeam(equipeId, userId);
    }

    public boolean hasPendingRequestForCurrentUser(int equipeId) throws SQLException {
        int userId = getCurrentUserIdOrThrow();
        return hasPendingRequest(equipeId, userId);
    }

    public void cancelPendingJoinRequestForCurrentUser(int equipeId) throws SQLException {
        int userId = getCurrentUserIdOrThrow();
        String sql = "DELETE FROM join_request WHERE equipe_id = ? AND user_id = ? AND LOWER(status) = 'pending'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, userId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Aucune demande pending a supprimer pour cette equipe.");
            }
        }
    }

    public void leaveEquipe(int equipeId) throws SQLException {
        int userId = getCurrentUserIdOrThrow();
        String sql = "DELETE FROM equipe_user WHERE equipe_id = ? AND user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, userId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Vous n'etes pas membre de cette equipe ou action impossible.");
            }
        }
    }

    public void processJoinRequest(int requestId, boolean accepted, String motif) throws SQLException {
        int ownerId = getCurrentUserIdOrThrow();
        boolean initialAutoCommit = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false);

            JoinRequestMeta request = getJoinRequestForUpdate(requestId);
            if (request == null) {
                throw new SQLException("Demande introuvable.");
            }
            if (!"pending".equalsIgnoreCase(request.statut)) {
                throw new SQLException("Cette demande est deja traitee.");
            }

            EquipeMeta equipe = getEquipeMetaForUpdate(request.equipeId);
            if (equipe == null) {
                throw new SQLException("Equipe introuvable.");
            }
            if (equipe.ownerId != ownerId) {
                throw new SQLException("Seul le owner de l'equipe peut traiter cette demande.");
            }

            String finalStatus = accepted ? "accepted" : "rejected";

            if (accepted) {
                if (request.joueurId == ownerId) {
                    throw new SQLException("Le owner ne peut pas etre ajoute comme membre via demande.");
                }
                if (isUserMemberOfTeam(request.equipeId, request.joueurId)) {
                    throw new SQLException("Le joueur est deja membre de cette equipe.");
                }
                if (countMembers(request.equipeId) >= equipe.maxMembers) {
                    throw new SQLException("Equipe pleine: impossible d'accepter la demande.");
                }

                String timestampColumn = getEquipeUserTimestampColumn();
                String insertMember = timestampColumn != null
                        ? "INSERT INTO equipe_user(equipe_id, user_id, " + timestampColumn + ") VALUES (?, ?, NOW())"
                        : "INSERT INTO equipe_user(equipe_id, user_id) VALUES (?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(insertMember)) {
                    ps.setInt(1, request.equipeId);
                    ps.setInt(2, request.joueurId);
                    ps.executeUpdate();
                } catch (SQLException firstEx) {
                    if (firstEx.getMessage() != null && firstEx.getMessage().toLowerCase().contains("unknown column")) {
                        String retrySql = "INSERT INTO equipe_user(equipe_id, user_id) VALUES (?, ?)";
                        try (PreparedStatement retryPs = conn.prepareStatement(retrySql)) {
                            retryPs.setInt(1, request.equipeId);
                            retryPs.setInt(2, request.joueurId);
                            retryPs.executeUpdate();
                        }
                    } else {
                        throw firstEx;
                    }
                }
            }

            String updateRequest = "UPDATE join_request "
                    + "SET status = ?, updated_at = NOW(), updated_by_id = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateRequest)) {
                ps.setString(1, finalStatus);
                ps.setInt(2, ownerId);
                ps.setInt(3, requestId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(initialAutoCommit);
        }
    }

    public List<EquipeJoinRequest> getPendingRequestsForCurrentOwner() throws SQLException {
        return getPendingRequestsForOwner(getCurrentUserIdOrThrow());
    }

    public List<EquipeJoinRequest> getPendingRequestsForOwner(int ownerId) throws SQLException {
        List<EquipeJoinRequest> requests = new ArrayList<>();
        String sql = "SELECT r.*, u.nom AS joueur_nom, e.nom AS equipe_nom FROM join_request r "
                + "INNER JOIN equipe e ON e.id = r.equipe_id "
                + "LEFT JOIN `user` u ON u.id = r.user_id "
                + "WHERE e.owner_id = ? AND LOWER(r.status) = 'pending' "
                + "ORDER BY r.created_at ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapJoinRequest(rs));
                }
            }
        }

        return requests;
    }

    public List<tn.esprit.entities.EquipeMemberView> getMembersForOwnedTeams() throws SQLException {
        List<tn.esprit.entities.EquipeMemberView> members = new ArrayList<>();
        String timestampColumn = getEquipeUserTimestampColumn();
        String timestampSelect = timestampColumn == null ? "" : ", m." + timestampColumn + " AS joined_at";
        String orderBy = timestampColumn == null ? "" : "ORDER BY m." + timestampColumn + " DESC";
        String sql = "SELECT m.equipe_id, e.nom AS equipe_nom, m.user_id, u.nom AS joueur_nom, u.email AS joueur_email" + timestampSelect + " "
                + "FROM equipe_user m "
                + "JOIN equipe e ON e.id = m.equipe_id "
                + "JOIN `user` u ON u.id = m.user_id "
                + "WHERE e.owner_id = ? "
                + "AND u.id != ? "
                + "AND LOWER(u.roles) NOT LIKE '%admin%' "
                + "AND LOWER(u.email) <> 'admin@gmail.com' "
                + orderBy;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int ownerId = getCurrentUserIdOrThrow();
            ps.setInt(1, ownerId);
            ps.setInt(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp joinedAt = timestampColumn == null ? null : rs.getTimestamp("joined_at");
                    members.add(new tn.esprit.entities.EquipeMemberView(
                            null,
                            rs.getInt("equipe_id"),
                            rs.getInt("user_id"),
                            rs.getString("equipe_nom"),
                            rs.getString("joueur_nom"),
                            rs.getString("joueur_email"),
                            "Member",
                            joinedAt
                    ));
                }
            }
        }

        return members;
    }

    public List<tn.esprit.entities.EquipeMemberView> getOwnerTeamPlayersAndRequests() throws SQLException {
        int ownerId = getCurrentUserIdOrThrow();
        List<Equipe> owned = getOwnedEquipes(ownerId);
        List<tn.esprit.entities.EquipeMemberView> result = new ArrayList<>();
        if (owned.isEmpty()) {
            return result;
        }

        List<Integer> ownedIds = new ArrayList<>();
        for (Equipe equipe : owned) {
            ownedIds.add(equipe.getId());
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(ownedIds.size(), "?"));

        String pendingSql = "SELECT r.id AS request_id, r.equipe_id, e.nom AS equipe_nom, r.user_id AS joueur_id, u.nom AS joueur_nom, u.email AS joueur_email, r.created_at "
                + "FROM join_request r "
                + "JOIN equipe e ON e.id = r.equipe_id "
                + "JOIN `user` u ON u.id = r.user_id "
                + "WHERE e.owner_id = ? AND LOWER(r.status) = 'pending' "
                + "AND LOWER(u.roles) NOT LIKE '%admin%' "
                + "AND LOWER(u.email) <> 'admin@gmail.com' "
                + "ORDER BY r.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(pendingSql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new tn.esprit.entities.EquipeMemberView(
                            rs.getInt("request_id"),
                            rs.getInt("equipe_id"),
                            rs.getInt("joueur_id"),
                            rs.getString("equipe_nom"),
                            rs.getString("joueur_nom"),
                            rs.getString("joueur_email"),
                            "Pending",
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }

        String timestampColumn = getEquipeUserTimestampColumn();
        String timestampSelect = timestampColumn == null ? "" : ", m." + timestampColumn + " AS joined_at";
        String orderBy = timestampColumn == null ? "" : "ORDER BY m." + timestampColumn + " DESC";
        String memberSql = "SELECT m.equipe_id, e.nom AS equipe_nom, m.user_id, u.nom AS joueur_nom, u.email AS joueur_email" + timestampSelect + " "
                + "FROM equipe_user m "
                + "JOIN equipe e ON e.id = m.equipe_id "
                + "JOIN `user` u ON u.id = m.user_id "
                + "WHERE e.owner_id = ? AND u.id != ? "
                + "AND LOWER(u.roles) NOT LIKE '%admin%' "
                + "AND LOWER(u.email) <> 'admin@gmail.com' "
                + orderBy;
        try (PreparedStatement ps = conn.prepareStatement(memberSql)) {
            ps.setInt(1, ownerId);
            ps.setInt(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp joinedAt = timestampColumn == null ? null : rs.getTimestamp("joined_at");
                    result.add(new tn.esprit.entities.EquipeMemberView(
                            null,
                            rs.getInt("equipe_id"),
                            rs.getInt("user_id"),
                            rs.getString("equipe_nom"),
                            rs.getString("joueur_nom"),
                            rs.getString("joueur_email"),
                            "Member",
                            joinedAt
                    ));
                }
            }
        }
        // ── Invited players (from team_invitation) ──
        String invitedSql = "SELECT i.id AS inv_id, i.equipe_id, e.nom AS equipe_nom, i.user_id AS joueur_id, "
                + "u.nom AS joueur_nom, u.email AS joueur_email, i.created_at "
                + "FROM team_invitation i "
                + "JOIN equipe e ON e.id = i.equipe_id "
                + "JOIN `user` u ON u.id = i.user_id "
                + "WHERE e.owner_id = ? AND LOWER(i.status) = 'invited' "
                + "ORDER BY i.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(invitedSql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new tn.esprit.entities.EquipeMemberView(
                            null,
                            rs.getInt("equipe_id"),
                            rs.getInt("joueur_id"),
                            rs.getString("equipe_nom"),
                            rs.getString("joueur_nom"),
                            rs.getString("joueur_email"),
                            "Invited",
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }

        // ── Available players (not member, not pending, not invited) ──
        String availableSql = "SELECT u.id AS joueur_id, u.nom AS joueur_nom, u.email AS joueur_email "
                + "FROM `user` u "
                + "WHERE u.id != ? "
                + "AND LOWER(u.roles) NOT LIKE '%admin%' "
                + "AND LOWER(u.email) <> 'admin@gmail.com' "
                + "AND u.id NOT IN (SELECT user_id FROM equipe_user WHERE equipe_id IN (" + placeholders + ")) "
                + "AND u.id NOT IN (SELECT user_id FROM join_request WHERE equipe_id IN (" + placeholders + ") AND LOWER(status) = 'pending') "
                + "AND u.id NOT IN (SELECT user_id FROM team_invitation WHERE equipe_id IN (" + placeholders + ") AND LOWER(status) = 'invited') "
                + "ORDER BY u.nom ASC";
        try (PreparedStatement ps = conn.prepareStatement(availableSql)) {
            int idx = 1;
            ps.setInt(idx++, ownerId);
            for (Integer ownedId : ownedIds) {
                ps.setInt(idx++, ownedId);
            }
            for (Integer ownedId : ownedIds) {
                ps.setInt(idx++, ownedId);
            }
            for (Integer ownedId : ownedIds) {
                ps.setInt(idx++, ownedId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new tn.esprit.entities.EquipeMemberView(
                            null,
                            -1,
                            rs.getInt("joueur_id"),
                            "-",
                            rs.getString("joueur_nom"),
                            rs.getString("joueur_email"),
                            "Available",
                            null
                    ));
                }
            }
        }

        return result;
    }

    public void addMemberToFirstOwnedEquipe(int joueurId) throws SQLException {
        int ownerId = getCurrentUserIdOrThrow();
        List<Equipe> owned = getOwnedEquipes(ownerId);
        if (owned.isEmpty()) {
            throw new SQLException("Aucune équipe trouvee pour ajouter le joueur.");
        }
        Equipe equipe = owned.get(0);
        if (isUserMemberOfTeam(equipe.getId(), joueurId)) {
            throw new SQLException("Le joueur est deja membre de cette equipe.");
        }
        if (countMembers(equipe.getId()) >= equipe.getMaxMembers()) {
            throw new SQLException("Equipe pleine: impossible d'ajouter un membre.");
        }

        String timestampColumn = getEquipeUserTimestampColumn();
        String sql = timestampColumn != null
                ? "INSERT INTO equipe_user(equipe_id, user_id, " + timestampColumn + ") VALUES (?, ?, NOW())"
                : "INSERT INTO equipe_user(equipe_id, user_id) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipe.getId());
            ps.setInt(2, joueurId);
            ps.executeUpdate();
        } catch (SQLException firstEx) {
            if (firstEx.getMessage() != null && firstEx.getMessage().toLowerCase().contains("unknown column")) {
                String retrySql = "INSERT INTO equipe_user(equipe_id, user_id) VALUES (?, ?)";
                try (PreparedStatement retryPs = conn.prepareStatement(retrySql)) {
                    retryPs.setInt(1, equipe.getId());
                    retryPs.setInt(2, joueurId);
                    retryPs.executeUpdate();
                    return;
                }
            }
            throw firstEx;
        }
    }

    public void removeMemberFromEquipe(int equipeId, int joueurId) throws SQLException {
        int ownerId = getCurrentUserIdOrThrow();
        String sql = "DELETE m FROM equipe_user m "
                + "JOIN equipe e ON e.id = m.equipe_id "
                + "WHERE m.equipe_id = ? AND m.user_id = ? AND e.owner_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, joueurId);
            ps.setInt(3, ownerId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Membre introuvable ou action non autorisee.");
            }
        }
    }

    public List<Equipe> getOwnedEquipes(int ownerId) throws SQLException {
        List<Equipe> equipes = new ArrayList<>();
        String sql = "SELECT * FROM equipe WHERE owner_id = ? ORDER BY id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipes.add(new Equipe(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getInt("max_members"),
                            rs.getString("logo")
                    ));
                }
            }
        }

        return equipes;
    }

    public List<Equipe> getJoinedEquipes(int userId) throws SQLException {
        List<Equipe> equipes = new ArrayList<>();
        String sql = "SELECT e.* FROM equipe e "
                + "INNER JOIN equipe_user m ON m.equipe_id = e.id "
                + "WHERE m.user_id = ? ORDER BY e.id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipes.add(new Equipe(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getInt("max_members"),
                            rs.getString("logo")
                    ));
                }
            }
        }

        return equipes;
    }

    public int countMembers(int equipeId) throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM equipe_user WHERE equipe_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("c");
                }
            }
        }
        return 0;
    }

    public OwnerDashboardStats getOwnerDashboardStatsForCurrentOwner() throws SQLException {
        return getOwnerDashboardStats(getCurrentUserIdOrThrow());
    }

    public OwnerDashboardStats getOwnerDashboardStats(int ownerId) throws SQLException {
        OwnerDashboardStats stats = new OwnerDashboardStats();

        List<Equipe> owned = getOwnedEquipes(ownerId);
        List<Equipe> joined = getJoinedEquipes(ownerId);

        stats.setOwnedTeamsCount(owned.size());
        stats.setJoinedTeamsCount(joined.size());
        stats.setPendingRequestsCount(getPendingRequestsForOwner(ownerId).size());

        Set<Integer> ownedIds = new HashSet<>();
        boolean fullTeam = false;
        int totalMembers = 0;
        int totalMaxMembers = 0;
        for (Equipe equipe : owned) {
            ownedIds.add(equipe.getId());
            int members = countMembers(equipe.getId());
            totalMembers += members;
            totalMaxMembers += equipe.getMaxMembers();
            if (members >= equipe.getMaxMembers()) {
                fullTeam = true;
            }
        }
        stats.setHasFullTeam(fullTeam);
        stats.setTotalMembers(totalMembers);
        stats.setTotalMaxMembers(totalMaxMembers);

        if (ownedIds.isEmpty()) {
            stats.setWinRate(0);
            return stats;
        }

        int wins = 0;
        int draws = 0;
        int losses = 0;
        int finished = 0;

        String placeholders = String.join(",", java.util.Collections.nCopies(ownedIds.size(), "?"));
        String sql = "SELECT id, equipe1_id, equipe2_id, score_team1, score_team2 "
                + "FROM match_game WHERE LOWER(statut) IN ('finished', 'termine') AND (equipe1_id IN (" + placeholders + ") "
                + "OR equipe2_id IN (" + placeholders + "))";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (Integer teamId : ownedIds) {
                ps.setInt(index++, teamId);
            }
            for (Integer teamId : ownedIds) {
                ps.setInt(index++, teamId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer s1 = (Integer) rs.getObject("score_team1");
                    Integer s2 = (Integer) rs.getObject("score_team2");
                    if (s1 == null || s2 == null) {
                        continue;
                    }

                    int eq1 = rs.getInt("equipe1_id");
                    int eq2 = rs.getInt("equipe2_id");
                    boolean eq1Owned = ownedIds.contains(eq1);
                    boolean eq2Owned = ownedIds.contains(eq2);

                    if (eq1Owned && eq2Owned) {
                        continue;
                    }

                    finished++;
                    if (eq1Owned) {
                        if (s1 > s2) {
                            wins++;
                        } else if (s1.equals(s2)) {
                            draws++;
                        } else {
                            losses++;
                        }
                    } else {
                        if (s2 > s1) {
                            wins++;
                        } else if (s1.equals(s2)) {
                            draws++;
                        } else {
                            losses++;
                        }
                    }
                }
            }
        }

        stats.setWins(wins);
        stats.setDraws(draws);
        stats.setLosses(losses);
        stats.setTotalFinishedMatches(finished);
        stats.setWinRate(finished == 0 ? 0 : round2((wins * 100.0) / finished));
        return stats;
    }

    public List<EquipeStanding> getGlobalRanking() throws SQLException {
        List<Equipe> equipes = getAll();
        Map<Integer, EquipeStanding> standings = new HashMap<>();

        for (Equipe equipe : equipes) {
            EquipeStanding row = new EquipeStanding();
            row.setEquipeId(equipe.getId());
            row.setEquipeNom(equipe.getNom());
            standings.put(equipe.getId(), row);
        }

        String sql = "SELECT equipe1_id, equipe2_id, score_team1, score_team2 "
                + "FROM match_game WHERE LOWER(statut) IN ('finished', 'termine')";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int equipe1Id = rs.getInt("equipe1_id");
                int equipe2Id = rs.getInt("equipe2_id");
                Integer s1 = (Integer) rs.getObject("score_team1");
                Integer s2 = (Integer) rs.getObject("score_team2");

                if (s1 == null || s2 == null) {
                    continue;
                }

                EquipeStanding team1 = standings.get(equipe1Id);
                EquipeStanding team2 = standings.get(equipe2Id);
                if (team1 == null || team2 == null) {
                    continue;
                }

                team1.setMj(team1.getMj() + 1);
                team2.setMj(team2.getMj() + 1);

                team1.setBp(team1.getBp() + s1);
                team1.setBc(team1.getBc() + s2);
                team2.setBp(team2.getBp() + s2);
                team2.setBc(team2.getBc() + s1);

                if (s1 > s2) {
                    team1.setV(team1.getV() + 1);
                    team2.setP(team2.getP() + 1);
                } else if (s1.equals(s2)) {
                    team1.setN(team1.getN() + 1);
                    team2.setN(team2.getN() + 1);
                } else {
                    team2.setV(team2.getV() + 1);
                    team1.setP(team1.getP() + 1);
                }
            }
        }

        List<EquipeStanding> result = new ArrayList<>(standings.values());
        for (EquipeStanding row : result) {
            row.setPoints(row.getV() * 3 + row.getN());
            row.setDiff(row.getBp() - row.getBc());
            row.setPpm(row.getMj() == 0 ? 0 : round2((double) row.getPoints() / row.getMj()));
            row.setBadge(resolveBadge(row.getPpm()));
        }

        result.sort(
            Comparator.comparingDouble(EquipeStanding::getPpm).reversed()
                .thenComparing(Comparator.comparingInt(EquipeStanding::getDiff).reversed())
                .thenComparing(Comparator.comparingInt(EquipeStanding::getBp).reversed())
                .thenComparing(Comparator.comparingInt(EquipeStanding::getV).reversed())
                .thenComparing(e -> e.getEquipeNom() == null ? "" : e.getEquipeNom())
        );

        int rank = 1;
        for (EquipeStanding row : result) {
            row.setRank(rank++);
        }

        return result;
    }

    private String resolveBadge(double ppm) {
        if (ppm >= 2.5) {
            return "Elite";
        }
        if (ppm >= 1.5) {
            return "Competitive";
        }
        return "Beginner";
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private boolean hasPendingRequest(int equipeId, int joueurId) throws SQLException {
        String sql = "SELECT 1 FROM join_request WHERE equipe_id = ? AND user_id = ? AND LOWER(status) = 'pending' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, joueurId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private EquipeMeta getEquipeMeta(int equipeId) throws SQLException {
        String sql = "SELECT id, owner_id, max_members FROM equipe WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new EquipeMeta(rs.getInt("owner_id"), rs.getInt("max_members"));
                }
            }
        }
        return null;
    }

    private EquipeMeta getEquipeMetaForUpdate(int equipeId) throws SQLException {
        String sql = "SELECT id, owner_id, max_members FROM equipe WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new EquipeMeta(rs.getInt("owner_id"), rs.getInt("max_members"));
                }
            }
        }
        return null;
    }

    private JoinRequestMeta getJoinRequestForUpdate(int requestId) throws SQLException {
        String sql = "SELECT id, equipe_id, user_id, status FROM join_request WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new JoinRequestMeta(rs.getInt("equipe_id"), rs.getInt("user_id"), rs.getString("status"));
                }
            }
        }
        return null;
    }

    private EquipeJoinRequest mapJoinRequest(ResultSet rs) throws SQLException {
        return new EquipeJoinRequest(
                rs.getInt("id"),
                rs.getInt("equipe_id"),
                rs.getInt("user_id"),
                rs.getString("status"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at"),
                (Integer) rs.getObject("created_by_id"),
                (Integer) rs.getObject("updated_by_id"),
                null,
                rs.getString("joueur_nom"),
                rs.getString("equipe_nom")
        );
    }

    private boolean isUserMemberOfTeam(int equipeId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM equipe_user WHERE equipe_id = ? AND user_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int getCurrentUserIdOrThrow() throws SQLException {
        User current = SessionManager.getCurrentUser();
        if (current == null || current.getId() <= 0) {
            throw new SQLException("Utilisateur non connecte.");
        }
        return current.getId();
    }

    private String getEquipeUserTimestampColumn() throws SQLException {
        java.util.Set<String> columns = getEquipeUserColumns();
        if (columns.contains("joined_at")) {
            return "joined_at";
        }
        if (columns.contains("created_at")) {
            return "created_at";
        }
        if (columns.contains("added_at")) {
            return "added_at";
        }
        return null;
    }

    private java.util.Set<String> getEquipeUserColumns() throws SQLException {
        java.util.Set<String> columns = new java.util.HashSet<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "equipe_user", null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }

        if (columns.isEmpty()) {
            String sql = "SELECT * FROM equipe_user LIMIT 1";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                java.sql.ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    columns.add(rsmd.getColumnLabel(i).toLowerCase());
                }
            } catch (SQLException ignored) {
                // If unable to inspect via query, fall back to metadata only.
            }
        }

        return columns;
    }

    private void ensureAdvancedTables() throws SQLException {
        String createMember = "CREATE TABLE IF NOT EXISTS equipe_user ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "equipe_id INT NOT NULL, "
                + "user_id INT NOT NULL, "
                + "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "UNIQUE KEY uq_equipe_user (equipe_id, user_id), "
                + "CONSTRAINT fk_equipe_user_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_equipe_user_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE"
                + ")";

        String createRequest = "CREATE TABLE IF NOT EXISTS join_request ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "equipe_id INT NOT NULL, "
                + "user_id INT NOT NULL, "
                + "status VARCHAR(20) NOT NULL DEFAULT 'pending', "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "created_by_id INT NULL, "
                + "updated_by_id INT NULL, "
                + "CONSTRAINT fk_join_request_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_join_request_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_join_request_created_by FOREIGN KEY (created_by_id) REFERENCES `user`(id) ON DELETE SET NULL, "
                + "CONSTRAINT fk_join_request_updated_by FOREIGN KEY (updated_by_id) REFERENCES `user`(id) ON DELETE SET NULL"
                + ")";

        String createInvitation = "CREATE TABLE IF NOT EXISTS team_invitation ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "equipe_id INT NOT NULL, "
                + "user_id INT NOT NULL, "
                + "status VARCHAR(20) NOT NULL DEFAULT 'invited', "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "CONSTRAINT fk_team_inv_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_team_inv_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE"
                + ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createMember);
            stmt.execute(createRequest);
            stmt.execute(createInvitation);
        }
    }

    private static class EquipeMeta {
        private final int ownerId;
        private final int maxMembers;

        private EquipeMeta(int ownerId, int maxMembers) {
            this.ownerId = ownerId;
            this.maxMembers = maxMembers;
        }
    }

    private static class JoinRequestMeta {
        private final int equipeId;
        private final int joueurId;
        private final String statut;

        private JoinRequestMeta(int equipeId, int joueurId, String statut) {
            this.equipeId = equipeId;
            this.joueurId = joueurId;
            this.statut = statut;
        }
    }

    // ═══════════════════════════════════════════
    //  TEAM INVITATION SYSTEM
    // ═══════════════════════════════════════════

    public void createInvitation(int equipeId, int userId) throws SQLException {
        int ownerId = getCurrentUserIdOrThrow();

        EquipeMeta meta = getEquipeMeta(equipeId);
        if (meta == null) throw new SQLException("Equipe introuvable.");
        if (meta.ownerId != ownerId) throw new SQLException("Seul le owner peut inviter.");
        if (userId == ownerId) throw new SQLException("Vous ne pouvez pas vous inviter vous-même.");
        if (isUserMemberOfTeam(equipeId, userId)) throw new SQLException("Ce joueur est déjà membre.");
        if (hasInvitedUser(equipeId, userId)) throw new SQLException("Une invitation est déjà envoyée à ce joueur.");

        String sql = "INSERT INTO team_invitation(equipe_id, user_id, status) VALUES (?, ?, 'invited')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }

        // Send email in background thread
        try {
            String playerEmail = getUserEmail(userId);
            String equipeName = getEquipeNom(equipeId);
            String ownerName = getUserNom(ownerId);
            if (playerEmail != null && !playerEmail.isBlank()) {
                new Thread(() -> {
                    new EmailService().sendTeamInvitationEmail(playerEmail, equipeName, ownerName);
                }).start();
            }
        } catch (Exception ignored) {
            // Email is best-effort, don't fail the invitation
        }
    }

    public boolean hasInvitedUser(int equipeId, int userId) throws SQLException {
        String sql = "SELECT 1 FROM team_invitation WHERE equipe_id = ? AND user_id = ? AND LOWER(status) = 'invited' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<TeamInvitation> getInvitationsForCurrentUser() throws SQLException {
        int userId = getCurrentUserIdOrThrow();
        List<TeamInvitation> invitations = new ArrayList<>();
        String sql = "SELECT i.id, i.equipe_id, i.user_id, e.nom AS equipe_nom, "
                + "owner.nom AS owner_nom, i.status, i.created_at "
                + "FROM team_invitation i "
                + "JOIN equipe e ON e.id = i.equipe_id "
                + "JOIN `user` owner ON owner.id = e.owner_id "
                + "WHERE i.user_id = ? AND LOWER(i.status) = 'invited' "
                + "ORDER BY i.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    invitations.add(new TeamInvitation(
                        rs.getInt("id"),
                        rs.getInt("equipe_id"),
                        rs.getInt("user_id"),
                        rs.getString("equipe_nom"),
                        rs.getString("owner_nom"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return invitations;
    }

    public void respondToInvitation(int invitationId, boolean accepted) throws SQLException {
        int userId = getCurrentUserIdOrThrow();
        String checkSql = "SELECT equipe_id, user_id, status FROM team_invitation WHERE id = ?";
        int equipeId;
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, invitationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Invitation introuvable.");
                if (rs.getInt("user_id") != userId) throw new SQLException("Cette invitation ne vous appartient pas.");
                if (!"invited".equalsIgnoreCase(rs.getString("status"))) throw new SQLException("Invitation déjà traitée.");
                equipeId = rs.getInt("equipe_id");
            }
        }

        String newStatus = accepted ? "accepted" : "refused";
        String updateSql = "UPDATE team_invitation SET status = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, invitationId);
            ps.executeUpdate();
        }

        if (accepted) {
            EquipeMeta meta = getEquipeMeta(equipeId);
            if (meta != null && countMembers(equipeId) >= meta.maxMembers) {
                throw new SQLException("Equipe pleine, impossible de rejoindre.");
            }
            String timestampColumn = getEquipeUserTimestampColumn();
            String insertSql = timestampColumn != null
                    ? "INSERT INTO equipe_user(equipe_id, user_id, " + timestampColumn + ") VALUES (?, ?, NOW())"
                    : "INSERT INTO equipe_user(equipe_id, user_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, equipeId);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
        }
    }

    private String getUserEmail(int userId) throws SQLException {
        String sql = "SELECT email FROM `user` WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("email") : null;
            }
        }
    }

    private String getUserNom(int userId) throws SQLException {
        String sql = "SELECT nom FROM `user` WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("nom") : "Owner";
            }
        }
    }

    private String getEquipeNom(int equipeId) throws SQLException {
        String sql = "SELECT nom FROM equipe WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("nom") : "Équipe";
            }
        }
    }

    public String getOwnerNameByEquipeId(int equipeId) throws SQLException {
        String sql = "SELECT u.nom FROM `user` u JOIN equipe e ON e.owner_id = u.id WHERE e.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("nom") : "—";
            }
        }
    }
}
