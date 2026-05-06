package tn.esprit.services;

import tn.esprit.entities.VideoComment;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceVideoComment {

    private Connection conn() {
        return MyDatabase.getInstance().getConnection();
    }

    // ================= GET COMMENTS =================
    public List<VideoComment> findByVideo(int videoId) {

        List<VideoComment> list = new ArrayList<>();

        if (videoId <= 0) return list;

        String sql = "SELECT * FROM video_comment WHERE video_id=? ORDER BY created_at DESC";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {

            ps.setInt(1, videoId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ================= ADD COMMENT =================
    public boolean add(int videoId, String username, String body) {

        if (videoId <= 0 || body == null || body.trim().isEmpty()) return false;

        if (username == null || username.trim().isEmpty()) {
            username = "Guest";
        }

        String sql = "INSERT INTO video_comment (video_id, username, body, created_at) VALUES (?, ?, ?, NOW())";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {

            ps.setInt(1, videoId);
            ps.setString(2, username.trim());
            ps.setString(3, body.trim());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================= DELETE COMMENT =================
    public boolean deleteComment(int id) {

        String sql = "DELETE FROM video_comment WHERE id=?";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ================= MAPPER =================
    private VideoComment map(ResultSet rs) throws SQLException {

        return new VideoComment(
                rs.getInt("id"),
                rs.getInt("video_id"),
                rs.getString("username"),
                rs.getString("body"),
                rs.getTimestamp("created_at")
        );
    }
}