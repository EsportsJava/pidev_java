package tn.esprit.services;

import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class ServiceTournoiInscription {

    private final Connection conn;

    public ServiceTournoiInscription() {
        this.conn = MyDatabase.getInstance().getConnection();
    }

    public void ensureTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS tournoi_inscription (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  tournoi_id INT NOT NULL,
                  user_id INT NULL,
                  nom VARCHAR(255) NULL,
                  email VARCHAR(255) NULL,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  CONSTRAINT fk_ti_tournoi FOREIGN KEY (tournoi_id) REFERENCES tournoi(id) ON DELETE CASCADE,
                  UNIQUE KEY uk_ti_unique (tournoi_id, user_id, email)
                )
                """;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public boolean existsForUser(int tournoiId, Integer userId, String email) throws SQLException {
        String sql = "SELECT 1 FROM tournoi_inscription WHERE tournoi_id=? AND (user_id=? OR (user_id IS NULL AND email=?)) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tournoiId);
            ps.setObject(2, userId);
            ps.setString(3, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countByTournoi(int tournoiId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tournoi_inscription WHERE tournoi_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tournoiId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public TournoiInscriptionSnapshot getSnapshotByTournoi(int tournoiId) throws SQLException {
        ensureTable();
        String sql = """
                SELECT COUNT(*) AS c, MIN(created_at) AS first_at
                FROM tournoi_inscription
                WHERE tournoi_id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tournoiId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("c");
                    java.sql.Timestamp firstAtTs = rs.getTimestamp("first_at");
                    LocalDateTime firstAt = firstAtTs == null ? null : firstAtTs.toLocalDateTime();
                    return new TournoiInscriptionSnapshot(count, firstAt);
                }
            }
        }
        return new TournoiInscriptionSnapshot(0, null);
    }

    public void inscrire(int tournoiId, Integer userId, String nom, String email) throws SQLException {
        ensureTable();
        String sql = "INSERT INTO tournoi_inscription(tournoi_id, user_id, nom, email) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tournoiId);
            ps.setObject(2, userId);
            ps.setString(3, nom);
            ps.setString(4, email);
            ps.executeUpdate();
        }
    }

    public Map<Integer, Integer> countByUserByJeu(int userId) throws SQLException {
        ensureTable();
        String sql = """
                SELECT t.jeu_id, COUNT(*) AS c
                FROM tournoi_inscription ti
                JOIN tournoi t ON t.id = ti.tournoi_id
                WHERE ti.user_id = ?
                GROUP BY t.jeu_id
                """;
        Map<Integer, Integer> out = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getInt("jeu_id"), rs.getInt("c"));
                }
            }
        }
        return out;
    }

    public DayOfWeek mostFrequentDayByUser(int userId) throws SQLException {
        ensureTable();
        String sql = """
                SELECT t.date_debut
                FROM tournoi_inscription ti
                JOIN tournoi t ON t.id = ti.tournoi_id
                WHERE ti.user_id = ? AND t.date_debut IS NOT NULL
                ORDER BY ti.created_at DESC
                LIMIT 50
                """;
        Map<DayOfWeek, Integer> freq = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Date d = rs.getDate("date_debut");
                    if (d == null) {
                        continue;
                    }
                    LocalDate ld = (d instanceof java.sql.Date sd)
                            ? sd.toLocalDate()
                            : d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    DayOfWeek day = ld.getDayOfWeek();
                    freq.put(day, freq.getOrDefault(day, 0) + 1);
                }
            }
        }
        DayOfWeek best = null;
        int max = -1;
        for (Map.Entry<DayOfWeek, Integer> e : freq.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    public record TournoiInscriptionSnapshot(int count, LocalDateTime firstInscriptionAt) {
    }
}

