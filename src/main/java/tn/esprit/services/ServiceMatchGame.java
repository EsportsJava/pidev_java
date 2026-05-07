package  tn.esprit.services;

import  tn.esprit.entities.MatchGame;
import  tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceMatchGame implements IService<MatchGame> {
    private Connection conn;

    public ServiceMatchGame() {
        conn = MyDatabase.getInstance().getConnection();
        if (conn != null) {
            try {
                ensureRoundRobinTables();
            } catch (SQLException e) {
                throw new RuntimeException("Initialisation des tables d'inscription equipe/tournoi impossible: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void ajouter(MatchGame matchGame) throws SQLException {
        String sql = "INSERT INTO match_game(date_match, score_team1, score_team2, statut, equipe1_id, equipe2_id, tournoi_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, matchGame.getDateMatch());
            ps.setObject(2, matchGame.getScoreTeam1());
            ps.setObject(3, matchGame.getScoreTeam2());
            ps.setString(4, matchGame.getStatut());
            ps.setInt(5, matchGame.getEquipe1Id());
            ps.setInt(6, matchGame.getEquipe2Id());
            ps.setInt(7, matchGame.getTournoiId());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(MatchGame matchGame) throws SQLException {
        String sql = "UPDATE match_game SET date_match=?, score_team1=?, score_team2=?, statut=?, equipe1_id=?, equipe2_id=?, tournoi_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, matchGame.getDateMatch());
            ps.setObject(2, matchGame.getScoreTeam1());
            ps.setObject(3, matchGame.getScoreTeam2());
            ps.setString(4, matchGame.getStatut());
            ps.setInt(5, matchGame.getEquipe1Id());
            ps.setInt(6, matchGame.getEquipe2Id());
            ps.setInt(7, matchGame.getTournoiId());
            ps.setInt(8, matchGame.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM match_game WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<MatchGame> getAll() throws SQLException {
        String sql = "SELECT * FROM match_game";
        List<MatchGame> matchGames = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                MatchGame matchGame = new MatchGame(
                        rs.getInt("id"),
                        rs.getTimestamp("date_match"),
                        (Integer) rs.getObject("score_team1"),
                        (Integer) rs.getObject("score_team2"),
                        rs.getString("statut"),
                        rs.getInt("equipe1_id"),
                        rs.getInt("equipe2_id"),
                        rs.getInt("tournoi_id")
                );
                matchGames.add(matchGame);
            }
        }

        return matchGames;
    }

    public List<MatchGame> getByTournoi(int tournoiId) throws SQLException {
        String sql = "SELECT * FROM match_game WHERE tournoi_id = ? ORDER BY date_match ASC";
        List<MatchGame> matchGames = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tournoiId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    matchGames.add(new MatchGame(
                            rs.getInt("id"),
                            rs.getTimestamp("date_match"),
                            (Integer) rs.getObject("score_team1"),
                            (Integer) rs.getObject("score_team2"),
                            rs.getString("statut"),
                            rs.getInt("equipe1_id"),
                            rs.getInt("equipe2_id"),
                            rs.getInt("tournoi_id")
                    ));
                }
            }
        }
        return matchGames;
    }


    public List<Integer> getRegisteredEquipeIds(int tournoiId) throws SQLException {
        List<Integer> equipeIds = new ArrayList<>();
        String sql = "SELECT DISTINCT equipe_id FROM tournoi_inscription WHERE tournoi_id = ? AND equipe_id IS NOT NULL ORDER BY equipe_id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tournoiId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipeIds.add(rs.getInt("equipe_id"));
                }
            }
        }
        return equipeIds;
    }

    public int countMatchesByTournoi(int tournoiId) throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM match_game WHERE tournoi_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tournoiId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("c");
                }
            }
        }
        return 0;
    }

    public int generateRoundRobinMatches(int tournoiId, boolean regenerate) throws SQLException {
        List<Integer> equipeIds = getRegisteredEquipeIds(tournoiId);
        if (equipeIds.size() < 2) {
            throw new SQLException("Generation impossible: minimum 2 equipes inscrites.");
        }

        int existing = countMatchesByTournoi(tournoiId);
        if (existing > 0 && !regenerate) {
            throw new SQLException("Generation bloquee: des matchs existent deja pour ce tournoi.");
        }

        boolean initialAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            if (regenerate && existing > 0) {
                try (PreparedStatement delete = conn.prepareStatement("DELETE FROM match_game WHERE tournoi_id = ?")) {
                    delete.setInt(1, tournoiId);
                    delete.executeUpdate();
                }
            }

            Collections.sort(equipeIds);
            int created = 0;
            Timestamp now = new Timestamp(System.currentTimeMillis());
            long base = now.getTime();

            String insert = "INSERT INTO match_game(date_match, score_team1, score_team2, statut, equipe1_id, equipe2_id, tournoi_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

            for (int i = 0; i < equipeIds.size(); i++) {
                for (int j = i + 1; j < equipeIds.size(); j++) {
                    Timestamp matchDate = new Timestamp(base + (created * 3600_000L));
                    try (PreparedStatement ps = conn.prepareStatement(insert)) {
                        ps.setTimestamp(1, matchDate);
                        ps.setInt(2, 0);
                        ps.setInt(3, 0);
                        ps.setString(4, "Planifié");
                        ps.setInt(5, equipeIds.get(i));
                        ps.setInt(6, equipeIds.get(j));
                        ps.setInt(7, tournoiId);
                        ps.executeUpdate();
                    }
                    created++;
                }
            }

            conn.commit();
            return created;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(initialAutoCommit);
        }
    }

    private void ensureRoundRobinTables() throws SQLException {
        String createInscriptionTournoi = "CREATE TABLE IF NOT EXISTS inscription_tournoi ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "date_inscription DATETIME NOT NULL, "
                + "tournoi_id INT NOT NULL, "
                + "equipe_id INT NOT NULL, "
                + "UNIQUE KEY uq_inscription_tournoi (tournoi_id, equipe_id), "
                + "INDEX idx_inscription_tournoi_tournoi (tournoi_id), "
                + "INDEX idx_inscription_tournoi_equipe (equipe_id), "
                + "CONSTRAINT fk_inscription_tournoi_tournoi FOREIGN KEY (tournoi_id) REFERENCES tournoi(id), "
                + "CONSTRAINT fk_inscription_tournoi_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id)"
                + ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createInscriptionTournoi);
        }
    }
}
