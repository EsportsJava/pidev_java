package  tn.esprit.services;

import  tn.esprit.entities.Jeu;
import  tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceJeu implements IService<Jeu> {
    private Connection conn;
    private final boolean hasLienOfficielColumn;

    public ServiceJeu() {
        conn = MyDatabase.getInstance().getConnection();
        hasLienOfficielColumn = detectLienOfficielColumn();
    }

    @Override
    public void ajouter(Jeu jeu) throws SQLException {
        String sql = hasLienOfficielColumn
                ? "INSERT INTO jeu(nom, genre, plateforme, description, statut, lien_officiel) VALUES (?, ?, ?, ?, ?, ?)"
                : "INSERT INTO jeu(nom, genre, plateforme, description, statut) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jeu.getNom());
            ps.setString(2, jeu.getGenre());
            ps.setString(3, jeu.getPlateforme());
            ps.setString(4, jeu.getDescription());
            ps.setString(5, jeu.getStatut());
            if (hasLienOfficielColumn) {
                ps.setString(6, jeu.getLienOfficiel());
            }
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Jeu jeu) throws SQLException {
        String sql = hasLienOfficielColumn
                ? "UPDATE jeu SET nom=?, genre=?, plateforme=?, description=?, statut=?, lien_officiel=? WHERE id=?"
                : "UPDATE jeu SET nom=?, genre=?, plateforme=?, description=?, statut=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jeu.getNom());
            ps.setString(2, jeu.getGenre());
            ps.setString(3, jeu.getPlateforme());
            ps.setString(4, jeu.getDescription());
            ps.setString(5, jeu.getStatut());
            if (hasLienOfficielColumn) {
                ps.setString(6, jeu.getLienOfficiel());
                ps.setInt(7, jeu.getId());
            } else {
                ps.setInt(6, jeu.getId());
            }
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
                String lienOfficiel = hasLienOfficielColumn ? rs.getString("lien_officiel") : "";
                Jeu jeu = new Jeu(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("genre"),
                        rs.getString("plateforme"),
                        rs.getString("description"),
                        rs.getString("statut"),
                        lienOfficiel
                );
                jeux.add(jeu);
            }
        }

        return jeux;
    }

    private boolean detectLienOfficielColumn() {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            String[] tableCandidates = {"jeu", "Jeu", "JEU"};
            String[] colCandidates = {"lien_officiel", "LIEN_OFFICIEL"};
            for (String table : tableCandidates) {
                for (String col : colCandidates) {
                    try (ResultSet rs = meta.getColumns(null, null, table, col)) {
                        if (rs.next()) {
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
            // In case metadata is unavailable, fall back to legacy schema.
        }
        return false;
    }
}
