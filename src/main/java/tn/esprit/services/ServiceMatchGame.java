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
        try {
            ensureRoundRobinTables();
        } catch (SQLException e) {
            throw new RuntimeException("Initialisation de la table tournoi_equipe impossible: " + e.getMessage(), e);
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

    public void registerEquipeToTournoi(int tournoiId, int equipeId) throws SQLException {
        String sql = "INSERT IGNORE INTO tournoi_equipe(tournoi_id, equipe_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tournoiId);
            ps.setInt(2, equipeId);
            ps.executeUpdate();
        }
    }

    public int registerAllEquipesToTournoi(int tournoiId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String read = "SELECT id FROM equipe ORDER BY id ASC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(read)) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        }

        int inserted = 0;
        for (Integer teamId : ids) {
            String sql = "INSERT IGNORE INTO tournoi_equipe(tournoi_id, equipe_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, tournoiId);
                ps.setInt(2, teamId);
                inserted += ps.executeUpdate();
            }
        }
        return inserted;
    }

    public int seedSampleMatchGames() throws SQLException {
        List<Integer> equipeIds = new ArrayList<>();
        String readTeams = "SELECT id FROM equipe ORDER BY id ASC LIMIT 4";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(readTeams)) {
            while (rs.next()) {
                equipeIds.add(rs.getInt("id"));
            }
        }

        if (equipeIds.size() < 2) {
            throw new SQLException("Impossible de semer les matchs : au moins 2 équipes sont requises.");
        }

        Integer tournoiId = null;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM tournoi ORDER BY id ASC LIMIT 1")) {
            if (rs.next()) {
                tournoiId = rs.getInt("id");
            }
        }
        if (tournoiId == null) {
            throw new SQLException("Impossible de semer les matchs : aucun tournoi trouvé.");
        }

        String checkSql = "SELECT COUNT(*) AS c FROM match_game WHERE date_match = ? AND equipe1_id = ? AND equipe2_id = ? AND tournoi_id = ?";
        String insertSql = "INSERT INTO match_game(date_match, score_team1, score_team2, statut, equipe1_id, equipe2_id, tournoi_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        int inserted = 0;

        try (PreparedStatement check = conn.prepareStatement(checkSql);
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            Object[][] seeds = new Object[][]{
                {Timestamp.valueOf("2026-04-01 18:00:00"), 3, 1, "Termine", equipeIds.get(0), equipeIds.get(1)},
                {Timestamp.valueOf("2026-04-02 19:30:00"), 2, 2, "Finished", equipeIds.get(0), equipeIds.get(1)},
                {Timestamp.valueOf("2026-04-03 20:00:00"), 1, 0, "Termine", equipeIds.get(0), equipeIds.get(1)}
            };

            if (equipeIds.size() >= 3) {
                seeds = new Object[][]{
                    {Timestamp.valueOf("2026-04-01 18:00:00"), 3, 1, "Termine", equipeIds.get(0), equipeIds.get(1)},
                    {Timestamp.valueOf("2026-04-02 19:30:00"), 2, 2, "Finished", equipeIds.get(1), equipeIds.get(2)},
                    {Timestamp.valueOf("2026-04-03 20:00:00"), 1, 0, "Termine", equipeIds.get(0), equipeIds.get(2)},
                    {Timestamp.valueOf("2026-04-10 17:00:00"), 0, 0, "Planifie", equipeIds.get(0), equipeIds.get(1)}
                };
            }
            if (equipeIds.size() >= 4) {
                seeds = new Object[][]{
                    {Timestamp.valueOf("2026-04-01 18:00:00"), 3, 1, "Termine", equipeIds.get(0), equipeIds.get(1)},
                    {Timestamp.valueOf("2026-04-02 19:30:00"), 2, 2, "Finished", equipeIds.get(1), equipeIds.get(2)},
                    {Timestamp.valueOf("2026-04-03 20:00:00"), 1, 0, "Termine", equipeIds.get(0), equipeIds.get(2)},
                    {Timestamp.valueOf("2026-04-04 16:15:00"), 0, 2, "Finished", equipeIds.get(2), equipeIds.get(3)},
                    {Timestamp.valueOf("2026-04-10 17:00:00"), 0, 0, "Planifie", equipeIds.get(0), equipeIds.get(3)}
                };
            }

            for (Object[] seed : seeds) {
                check.setTimestamp(1, (Timestamp) seed[0]);
                check.setInt(2, (Integer) seed[4]);
                check.setInt(3, (Integer) seed[5]);
                check.setInt(4, tournoiId);
                try (ResultSet rs = check.executeQuery()) {
                    int count = rs.next() ? rs.getInt("c") : 0;
                    if (count > 0) {
                        continue;
                    }
                }

                ps.setTimestamp(1, (Timestamp) seed[0]);
                ps.setInt(2, (Integer) seed[1]);
                ps.setInt(3, (Integer) seed[2]);
                ps.setString(4, (String) seed[3]);
                ps.setInt(5, (Integer) seed[4]);
                ps.setInt(6, (Integer) seed[5]);
                ps.setInt(7, tournoiId);
                inserted += ps.executeUpdate();
            }
        }

        return inserted;
    }

    public List<Integer> getRegisteredEquipeIds(int tournoiId) throws SQLException {
        List<Integer> equipeIds = new ArrayList<>();
        String sql = "SELECT equipe_id FROM tournoi_equipe WHERE tournoi_id = ? ORDER BY equipe_id ASC";
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
                        ps.setString(4, "Scheduled");
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
        String sql = "CREATE TABLE IF NOT EXISTS tournoi_equipe ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "tournoi_id INT NOT NULL, "
                + "equipe_id INT NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "UNIQUE KEY uq_tournoi_equipe (tournoi_id, equipe_id), "
                + "CONSTRAINT fk_tournoi_equipe_tournoi FOREIGN KEY (tournoi_id) REFERENCES tournoi(id) ON DELETE CASCADE, "
                + "CONSTRAINT fk_tournoi_equipe_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id) ON DELETE CASCADE"
                + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
