package tn.esprit.services;

import tn.esprit.utils.MyDatabase;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceCommentReaction {

    private final Connection cnx;

    public ServiceCommentReaction() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public String getUserReaction(int commentId, int userId) {
        String sql = "SELECT reaction_type FROM comment_reaction WHERE comment_id=? AND user_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("reaction_type");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String toggleReaction(int commentId, int userId, String reactionType) {
        String current = getUserReaction(commentId, userId);
        if (reactionType.equals(current)) {
            deleteReaction(commentId, userId);
            return null;
        } else {
            upsertReaction(commentId, userId, reactionType);
            return reactionType;
        }
    }

    public Map<String, Integer> getReactionCounts(int commentId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT reaction_type, COUNT(*) as cnt FROM comment_reaction " +
                "WHERE comment_id=? GROUP BY reaction_type";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                counts.put(rs.getString("reaction_type"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return counts;
    }

    private void upsertReaction(int commentId, int userId, String reactionType) {
        String sql = "INSERT INTO comment_reaction (comment_id, user_id, reaction_type) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE reaction_type=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            ps.setInt(2, userId);
            ps.setString(3, reactionType);
            ps.setString(4, reactionType);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteReaction(int commentId, int userId) {
        String sql = "DELETE FROM comment_reaction WHERE comment_id=? AND user_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}