package tn.esprit.services;

import tn.esprit.utils.MyDatabase;
import tn.esprit.utils.BlogSchemaInitializer;
import java.sql.*;

public class ServiceBlogRating {

    private final Connection cnx;

    public ServiceBlogRating() {
        cnx = MyDatabase.getInstance().getConnection();
        BlogSchemaInitializer.ensureInitialized(cnx);
    }

    public void rateOrUpdate(int blogId, int userId, int rating) {
        String sql = "INSERT INTO rating (blog_id, user_id, value, created_at, updated_at) VALUES (?,?,?,NOW(),NOW()) " +
                "ON DUPLICATE KEY UPDATE value=?, updated_at=NOW()";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blogId);
            ps.setInt(2, userId);
            ps.setInt(3, rating);
            ps.setInt(4, rating);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double getAverageRating(int blogId) {
        String sql = "SELECT AVG(value) as avg_rating FROM rating WHERE blog_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blogId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("avg_rating");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public int getRatingCount(int blogId) {
        String sql = "SELECT COUNT(*) as cnt FROM rating WHERE blog_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blogId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getUserRating(int blogId, int userId) {
        String sql = "SELECT value FROM rating WHERE blog_id=? AND user_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blogId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("value");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
