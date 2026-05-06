package tn.esprit.entities;

import java.util.Date;

public class Tournoi {
    private int id;
    private String nom;
    private Date dateDebut;
    private Date dateFin;
    private String statut;
    private String type;
    private int maxParticipants;
    private double cagnotte;
    private Date dateInscriptionLimite;
    private double fraisInscription;
    private String description;
    private int jeuId;

    public Tournoi() {
    }

    public Tournoi(int id, String nom, Date dateDebut, Date dateFin, String statut, String type, 
                   int maxParticipants, double cagnotte, Date dateInscriptionLimite, 
                   double fraisInscription, String description, int jeuId) {
        this.id = id;
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.type = type;
        this.maxParticipants = maxParticipants;
        this.cagnotte = cagnotte;
        this.dateInscriptionLimite = dateInscriptionLimite;
        this.fraisInscription = fraisInscription;
        this.description = description;
        this.jeuId = jeuId;
    }

    public Tournoi(String nom, Date dateDebut, Date dateFin, String statut, String type, 
                   int maxParticipants, double cagnotte, Date dateInscriptionLimite, 
                   double fraisInscription, String description, int jeuId) {
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.type = type;
        this.maxParticipants = maxParticipants;
        this.cagnotte = cagnotte;
        this.dateInscriptionLimite = dateInscriptionLimite;
        this.fraisInscription = fraisInscription;
        this.description = description;
        this.jeuId = jeuId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public double getCagnotte() {
        return cagnotte;
    }

    public void setCagnotte(double cagnotte) {
        this.cagnotte = cagnotte;
    }

    public Date getDateInscriptionLimite() {
        return dateInscriptionLimite;
    }

    public void setDateInscriptionLimite(Date dateInscriptionLimite) {
        this.dateInscriptionLimite = dateInscriptionLimite;
    }

    public double getFraisInscription() {
        return fraisInscription;
    }

    public void setFraisInscription(double fraisInscription) {
        this.fraisInscription = fraisInscription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getJeuId() {
        return jeuId;
    }

    public void setJeuId(int jeuId) {
        this.jeuId = jeuId;
    }

    @Override
    public String toString() {
        return "Tournoi{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", statut='" + statut + '\'' +
                ", type='" + type + '\'' +
                ", maxParticipants=" + maxParticipants +
                ", cagnotte=" + cagnotte +
                ", jeuId=" + jeuId +
                '}';
    }
}
