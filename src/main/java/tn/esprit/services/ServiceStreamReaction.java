package tn.esprit.services;

import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ServiceStreamReaction {

    private Connection conn() {
        return MyDatabase.getInstance().getConnection();
    }

    private volatile boolean schemaChecked = false;

    /**
     * Certains dumps/DB existants ont `id` non auto-incrémenté -> INSERT échoue avec
     * "Field 'id' doesn't have a default value". On tente de corriger automatiquement une fois.
     */
    private void ensureSchema() {
        if (schemaChecked) {
            return;
        }
        schemaChecked = true;

        if (conn() == null) {
            return;
        }

        try (Statement st = conn().createStatement()) {
            st.executeUpdate("ALTER TABLE stream_reaction MODIFY id INT NOT NULL AUTO_INCREMENT");
        } catch (Exception ignored) {
            // table déjà OK, ou privilèges insuffisants -> on laisse les requêtes gérer l'erreur
        }
    }

    // emojis disponibles
    public static final String[] REACTIONS = {"👍", "❤️", "😂", "😮", "😢", "😡"};

    // =========================
    // 1️⃣ ADD REACTION
    // =========================
    public boolean addEmojiReaction(String emoji, String username, int streamId) {
        if (conn() == null) {
            System.err.println("ServiceStreamReaction: pas de connexion JDBC (vérifiez la base esport-db et les tables stream / stream_reaction).");
            return false;
        }
        ensureSchema();
        String sql = "INSERT INTO stream_reaction(type, username, stream_id, created_at) VALUES (?, ?, ?, NOW())";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, emoji);
            ps.setString(2, username);
            ps.setInt(3, streamId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("Field 'id'") && msg.contains("default value")) {
                // Fallback si on n'a pas les droits ALTER ou si le schéma reste non auto-incrémenté:
                // on génère un id manuellement (MAX(id)+1) et on insère avec id explicite.
                String sqlManual = "INSERT INTO stream_reaction(id, type, username, stream_id, created_at) VALUES (?, ?, ?, ?, NOW())";
                try (
                        Statement st = conn().createStatement();
                        ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM stream_reaction");
                        PreparedStatement ps2 = conn().prepareStatement(sqlManual)
                ) {
                    int nextId = 1;
                    if (rs.next()) {
                        nextId = rs.getInt(1);
                    }
                    ps2.setInt(1, nextId);
                    ps2.setString(2, emoji);
                    ps2.setString(3, username);
                    ps2.setInt(4, streamId);
                    return ps2.executeUpdate() > 0;
                } catch (Exception e2) {
                    System.err.println("ServiceStreamReaction retry INSERT échoué: " + e2.getMessage());
                    e2.printStackTrace();
                    return false;
                }
            }
            System.err.println("ServiceStreamReaction INSERT échoué: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // 2️⃣ TOTAL REACTIONS
    // =========================
    public int getTotalReactions(int streamId) {
        if (conn() == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM stream_reaction WHERE stream_id=?";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(0, streamId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // =========================
    // 3️⃣ COUNT BY EMOJI (OPTIONAL)
    // =========================
    public int countByType(String emoji, int streamId) {
        if (conn() == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM stream_reaction WHERE type=? AND stream_id=?";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, emoji);
            ps.setInt(2, streamId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}