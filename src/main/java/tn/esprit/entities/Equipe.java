package tn.esprit.entities;

public class Equipe {
    private int id;
    private String nom;
    private int maxMembers;
    private String logo;

    public Equipe() {
    }

    public Equipe(int id, String nom, int maxMembers, String logo) {
        this.id = id;
        this.nom = nom;
        this.maxMembers = maxMembers;
        this.logo = logo;
    }

    public Equipe(String nom, int maxMembers, String logo) {
        this.nom = nom;
        this.maxMembers = maxMembers;
        this.logo = logo;
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

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    @Override
    public String toString() {
        return "Equipe{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", maxMembers=" + maxMembers +
                '}';
    }
}
