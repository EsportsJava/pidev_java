package  tn.esprit.services;

import  tn.esprit.entities.Tournoi;
import  tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceTournoi implements IService<Tournoi> {
    private Connection conn;

    public ServiceTournoi() {
        conn = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Tournoi tournoi) throws SQLException {
        String sql = "INSERT INTO tournoi(nom, date_debut, date_fin, statut, type, max_participants, cagnotte, date_inscription_limite, frais_inscription, description, jeu_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tournoi.getNom());
            ps.setDate(2, new java.sql.Date(tournoi.getDateDebut().getTime()));
            ps.setDate(3, new java.sql.Date(tournoi.getDateFin().getTime()));
            ps.setString(4, tournoi.getStatut());
            ps.setString(5, tournoi.getType());
            ps.setInt(6, tournoi.getMaxParticipants());
            ps.setDouble(7, tournoi.getCagnotte());
            ps.setDate(8, new java.sql.Date(tournoi.getDateInscriptionLimite().getTime()));
            ps.setDouble(9, tournoi.getFraisInscription());
            ps.setString(10, tournoi.getDescription());
            ps.setInt(11, tournoi.getJeuId());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Tournoi tournoi) throws SQLException {
        String sql = "UPDATE tournoi SET nom=?, date_debut=?, date_fin=?, statut=?, type=?, max_participants=?, cagnotte=?, date_inscription_limite=?, frais_inscription=?, description=?, jeu_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tournoi.getNom());
            ps.setDate(2, new java.sql.Date(tournoi.getDateDebut().getTime()));
            ps.setDate(3, new java.sql.Date(tournoi.getDateFin().getTime()));
            ps.setString(4, tournoi.getStatut());
            ps.setString(5, tournoi.getType());
            ps.setInt(6, tournoi.getMaxParticipants());
            ps.setDouble(7, tournoi.getCagnotte());
            ps.setDate(8, new java.sql.Date(tournoi.getDateInscriptionLimite().getTime()));
            ps.setDouble(9, tournoi.getFraisInscription());
            ps.setString(10, tournoi.getDescription());
            ps.setInt(11, tournoi.getJeuId());
            ps.setInt(12, tournoi.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM tournoi WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Tournoi> getAll() throws SQLException {
        String sql = "SELECT * FROM tournoi";
        List<Tournoi> tournois = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Tournoi tournoi = new Tournoi(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin"),
                        rs.getString("statut"),
                        rs.getString("type"),
                        rs.getInt("max_participants"),
                        rs.getDouble("cagnotte"),
                        rs.getDate("date_inscription_limite"),
                        rs.getDouble("frais_inscription"),
                        rs.getString("description"),
                        rs.getInt("jeu_id")
                );
                tournois.add(tournoi);
            }
        }

        return tournois;
    }
}
