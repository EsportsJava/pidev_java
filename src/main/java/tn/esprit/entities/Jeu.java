package tn.esprit.entities;

public class Jeu {
    private int id;
    private String nom;
    private String genre;
    private String plateforme;
    private String description;
    private String statut;

    public Jeu() {
    }

    public Jeu(int id, String nom, String genre, String plateforme, String description, String statut) {
        this.id = id;
        this.nom = nom;
        this.genre = genre;
        this.plateforme = plateforme;
        this.description = description;
        this.statut = statut;
    }

    public Jeu(String nom, String genre, String plateforme, String description, String statut) {
        this.nom = nom;
        this.genre = genre;
        this.plateforme = plateforme;
        this.description = description;
        this.statut = statut;
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

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getPlateforme() {
        return plateforme;
    }

    public void setPlateforme(String plateforme) {
        this.plateforme = plateforme;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Jeu{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", genre='" + genre + '\'' +
                ", plateforme='" + plateforme + '\'' +
                ", description='" + description + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}
