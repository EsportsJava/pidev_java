package tn.esprit.services;

import tn.esprit.utils.MyDatabase;

import java.sql.*;

public class ServiceVideoReaction {

    private Connection conn() {
        return MyDatabase.getInstance().getConnection();
    }

    // ================= ADD REACTION =================
    public boolean addReaction(String emoji, String username, int videoId) {

        if (emoji == null || emoji.trim().isEmpty() || videoId <= 0) return false;

        if (username == null || username.trim().isEmpty()) {
            username = "Guest";
        }

        String sql = "INSERT INTO video_reaction (type, username, video_id, created_at) VALUES (?, ?, ?, NOW())";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {

            ps.setString(1, emoji.trim());
            ps.setString(2, username.trim());
            ps.setInt(3, videoId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================= TOTAL =================
    public int getTotalReactions(int videoId) {

        String sql = "SELECT COUNT(*) FROM video_reaction WHERE video_id=?";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {

            ps.setInt(1, videoId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}