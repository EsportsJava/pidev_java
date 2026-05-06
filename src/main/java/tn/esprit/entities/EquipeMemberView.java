package tn.esprit.entities;

import java.sql.Timestamp;

public class EquipeMemberView {
    private Integer requestId;
    private int equipeId;
    private int joueurId;
    private String equipeNom;
    private String joueurNom;
    private String joueurEmail;
    private String status;
    private Timestamp joinedAt;

    public EquipeMemberView() {
    }

    public EquipeMemberView(Integer requestId, int equipeId, int joueurId, String equipeNom, String joueurNom,
                             String joueurEmail, String status, Timestamp joinedAt) {
        this.requestId = requestId;
        this.equipeId = equipeId;
        this.joueurId = joueurId;
        this.equipeNom = equipeNom;
        this.joueurNom = joueurNom;
        this.joueurEmail = joueurEmail;
        this.status = status;
        this.joinedAt = joinedAt;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
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

    public String getEquipeNom() {
        return equipeNom;
    }

    public void setEquipeNom(String equipeNom) {
        this.equipeNom = equipeNom;
    }

    public String getJoueurNom() {
        return joueurNom;
    }

    public void setJoueurNom(String joueurNom) {
        this.joueurNom = joueurNom;
    }

    public String getJoueurEmail() {
        return joueurEmail;
    }

    public void setJoueurEmail(String joueurEmail) {
        this.joueurEmail = joueurEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }
}
