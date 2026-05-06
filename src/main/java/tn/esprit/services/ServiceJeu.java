package  tn.esprit.services;

import  tn.esprit.entities.Jeu;
import  tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceJeu implements IService<Jeu> {
    private Connection conn;

    public ServiceJeu() {
        conn = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Jeu jeu) throws SQLException {
        String sql = "INSERT INTO jeu(nom, genre, plateforme, description, statut) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jeu.getNom());
            ps.setString(2, jeu.getGenre());
            ps.setString(3, jeu.getPlateforme());
            ps.setString(4, jeu.getDescription());
            ps.setString(5, jeu.getStatut());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Jeu jeu) throws SQLException {
        String sql = "UPDATE jeu SET nom=?, genre=?, plateforme=?, description=?, statut=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jeu.getNom());
            ps.setString(2, jeu.getGenre());
            ps.setString(3, jeu.getPlateforme());
            ps.setString(4, jeu.getDescription());
            ps.setString(5, jeu.getStatut());
            ps.setInt(6, jeu.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM jeu WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Jeu> getAll() throws SQLException {
        String sql = "SELECT * FROM jeu";
        List<Jeu> jeux = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Jeu jeu = new Jeu(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("genre"),
                        rs.getString("plateforme"),
                        rs.getString("description"),
                        rs.getString("statut")
                );
                jeux.add(jeu);
            }
        }

        return jeux;
    }
}
