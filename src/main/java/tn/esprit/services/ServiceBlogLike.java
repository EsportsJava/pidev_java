package tn.esprit.services;

import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ServiceBlogLike {

    private final Connection cnx;

    public ServiceBlogLike() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public boolean toggleLike(int blogId, int userId) {
        if (hasLiked(blogId, userId)) {
            removeLike(blogId, userId);
            return false;
        } else {
            addLike(blogId, userId);
            return true;
        }
    }

    public boolean hasLiked(int blogId, int userId) {
        String sql = "SELECT id FROM blog_like WHERE blog_id=? AND user_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blogId);
            ps.setInt(2, userId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getLikeCount(int blogId) {
        String sql = "SELECT COUNT(*) AS like_count FROM blog_like WHERE blog_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blogId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("like_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void addLike(int blogId, int userId) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "INSERT INTO blog_like (blog_id, user_id) VALUES (?,?)")) {
            ps.setInt(1, blogId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeLike(int blogId, int userId) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM blog_like WHERE blog_id=? AND user_id=?")) {
            ps.setInt(1, blogId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
