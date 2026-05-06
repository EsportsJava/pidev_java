package tn.esprit.entities;

import java.sql.Timestamp;

public class EquipeJoinRequest {
    private int id;
    private int equipeId;
    private int joueurId;
    private String statut;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Integer createdById;
    private Integer updatedById;
    private String motif;
    private String joueurNom;
    private String equipeNom;

    public EquipeJoinRequest() {
    }

    public EquipeJoinRequest(int id, int equipeId, int joueurId, String statut, Timestamp createdAt,
                             Timestamp updatedAt, Integer createdById, Integer updatedById, String motif) {
        this.id = id;
        this.equipeId = equipeId;
        this.joueurId = joueurId;
        this.statut = statut;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdById = createdById;
        this.updatedById = updatedById;
        this.motif = motif;
    }

    public EquipeJoinRequest(int id, int equipeId, int joueurId, String statut, Timestamp createdAt,
                             Timestamp updatedAt, Integer createdById, Integer updatedById, String motif, String joueurNom, String equipeNom) {
        this(id, equipeId, joueurId, statut, createdAt, updatedAt, createdById, updatedById, motif);
        this.joueurNom = joueurNom;
        this.equipeNom = equipeNom;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEquipeId() {
        return equipeId;
    }

    public void setEquipeId(int equipeId) {
        this.equipeId = equipeId;
    }

    public int getJoueurId() {
        return joueurId;
    }

    public void setJoueurId(int joueurId) {
        this.joueurId = joueurId;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Integer createdById) {
        this.createdById = createdById;
    }

    public Integer getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(Integer updatedById) {
        this.updatedById = updatedById;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getJoueurNom() {
        return joueurNom;
    }

    public void setJoueurNom(String joueurNom) {
        this.joueurNom = joueurNom;
    }

    public String getEquipeNom() {
        return equipeNom;
    }

    public void setEquipeNom(String equipeNom) {
        this.equipeNom = equipeNom;
    }
}