package tn.esprit.services;

import  tn.esprit.entities.Equipe;
import  tn.esprit.entities.User;
import  tn.esprit.utils.MyDatabase;
import  tn.esprit.utils.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceEquipe implements IService<Equipe> {
    private Connection conn;

    public ServiceEquipe() {
        conn = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Equipe equipe) throws SQLException {
        String sql = "INSERT INTO equipe(nom, max_members, logo, owner_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, equipe.getNom());
            ps.setInt(2, equipe.getMaxMembers());
            ps.setString(3, equipe.getLogo());
            ps.setInt(4, resolveOwnerId());
            ps.executeUpdate();
        }
    }

    private int resolveOwnerId() throws SQLException {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && currentUser.getId() > 0) {
            return currentUser.getId();
        }

        String fallbackSql = "SELECT id FROM `user` ORDER BY id ASC LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(fallbackSql)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        throw new SQLException("Aucun utilisateur trouve pour owner_id. Creez un utilisateur puis reessayez.");
    }

    @Override
    public void modifier(Equipe equipe) throws SQLException {
        String sql = "UPDATE equipe SET nom=?, max_members=?, logo=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, equipe.getNom());
            ps.setInt(2, equipe.getMaxMembers());
            ps.setString(3, equipe.getLogo());
            ps.setInt(4, equipe.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM equipe WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Equipe> getAll() throws SQLException {
        String sql = "SELECT * FROM equipe";
        List<Equipe> equipes = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Equipe equipe = new Equipe(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getInt("max_members"),
                    rs.getString("logo")
                );
                equipes.add(equipe);
            }
        }

        return equipes;
    }

    /**
     * Règle simplifiée: l'utilisateur est considéré "dans une équipe" s'il est owner d'une équipe.
     * (A étendre plus tard avec une table de membres si besoin.)
     */
    public boolean isUserInAnyEquipe(int userId) throws SQLException {
        String sql = "SELECT 1 FROM equipe WHERE owner_id=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
