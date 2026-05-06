package tn.esprit.services;

import tn.esprit.entities.Stream;
import tn.esprit.utils.MyDatabase;

import java.sql.*;

public class ServiceStream {

    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    // ================= GET ACTIVE STREAM =================
    public Stream getActiveStream() {

        String sql = "SELECT * FROM stream WHERE is_active=1 LIMIT 1";

        try (PreparedStatement ps = cnx().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return new Stream(
                        rs.getInt("id"),
                        rs.getString("url"),
                        rs.getBoolean("is_active")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // ================= FORCE DEFAULT STREAM =================
    public void ensureStreamExists() {

        String sql = """
            INSERT INTO stream (url, is_active)
            SELECT 'http://100.89.37.94:8080/hls/match1.m3u8', 1
            WHERE NOT EXISTS (SELECT 1 FROM stream LIMIT 1)
        """;

        try (Statement st = cnx().createStatement()) {
            st.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}