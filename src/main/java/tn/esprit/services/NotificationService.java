package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.entities.User;
import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class NotificationService {

    private final Connection conn;

    public NotificationService() {
        this.conn = MyDatabase.getInstance().getConnection();
    }

    public void ensureTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS notification (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  user_id INT NOT NULL,
                  type VARCHAR(64) NOT NULL,
                  title VARCHAR(255) NOT NULL,
                  body TEXT NOT NULL,
                  ref_key VARCHAR(255) NULL,
                  is_read BOOLEAN NOT NULL DEFAULT FALSE,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE,
                  UNIQUE KEY uk_notification_dedup (user_id, type, ref_key)
                )
                """;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public void createNotificationIfAbsent(int userId, String type, String title, String body, String refKey) throws SQLException {
        ensureTable();
        String sql = "INSERT IGNORE INTO notification(user_id, type, title, body, ref_key) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setString(3, title);
            ps.setString(4, body);
            ps.setString(5, refKey);
            ps.executeUpdate();
        }
    }

    public JSONArray listByUser(int userId) throws SQLException {
        ensureTable();
        String sql = "SELECT id, type, title, body, is_read, created_at FROM notification WHERE user_id=? ORDER BY id DESC";
        JSONArray arr = new JSONArray();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject o = new JSONObject();
                    o.put("id", rs.getInt("id"));
                    o.put("type", rs.getString("type"));
                    o.put("title", rs.getString("title"));
                    o.put("body", rs.getString("body"));
                    o.put("isRead", rs.getBoolean("is_read"));
                    o.put("createdAt", String.valueOf(rs.getTimestamp("created_at")));
                    arr.put(o);
                }
            }
        }
        return arr;
    }

    public int unreadCount(int userId) throws SQLException {
        ensureTable();
        String sql = "SELECT COUNT(*) FROM notification WHERE user_id=? AND is_read=FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void markAllRead(int userId) throws SQLException {
        ensureTable();
        String sql = "UPDATE notification SET is_read=TRUE WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Génère des rappels "deadline proche" et "début tournoi" pour les tournois inscrits.
     */
    public void generateTournamentReminderNotifications(User user) throws SQLException {
        if (user == null || user.getId() <= 0) {
            return;
        }
        ensureTable();
        String sql = """
                SELECT t.id, t.nom, t.date_inscription_limite, t.date_debut
                FROM tournoi_inscription ti
                JOIN tournoi t ON t.id = ti.tournoi_id
                WHERE ti.user_id = ?
                """;
        LocalDate today = LocalDate.now();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int tournoiId = rs.getInt("id");
                    String nom = rs.getString("nom");
                    Date limite = rs.getDate("date_inscription_limite");
                    Date debut = rs.getDate("date_debut");

                    if (limite != null) {
                        LocalDate d = (limite instanceof java.sql.Date sd)
                                ? sd.toLocalDate()
                                : limite.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        if (!today.isAfter(d) && !today.plusDays(1).isBefore(d)) {
                            createNotificationIfAbsent(
                                    user.getId(),
                                    "deadline_reminder",
                                    "Rappel deadline",
                                    "Le tournoi \"" + nom + "\" ferme bientôt les inscriptions.",
                                    "deadline:" + tournoiId + ":" + d);
                        }
                    }
                    if (debut != null) {
                        LocalDate d = (debut instanceof java.sql.Date sd)
                                ? sd.toLocalDate()
                                : debut.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        if (today.equals(d) || today.plusDays(1).equals(d)) {
                            createNotificationIfAbsent(
                                    user.getId(),
                                    "tournament_start",
                                    "Début tournoi",
                                    "Le tournoi \"" + nom + "\" commence bientôt.",
                                    "start:" + tournoiId + ":" + d);
                        }
                    }
                }
            }
        }
    }
}

