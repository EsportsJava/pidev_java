package tn.esprit.services;

import tn.esprit.utils.MyDatabase;
import java.sql.*;

public class ServiceReport {

    private final Connection cnx;

    public ServiceReport() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    public boolean alreadyReported(int userId, String targetType, int targetId) {
        String sql = "SELECT id FROM report WHERE user_id=? AND target_type=? AND target_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, targetType);
            ps.setInt(3, targetId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void addReport(int userId, String targetType, int targetId, String reason) {
        String sql = "INSERT INTO report (user_id, target_type, target_id, reason, status) " +
                "VALUES (?,?,?,?,'pending')";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, targetType);
            ps.setInt(3, targetId);
            ps.setString(4, reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}