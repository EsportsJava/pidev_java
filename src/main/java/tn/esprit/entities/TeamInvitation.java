package tn.esprit.entities;

import java.sql.Timestamp;

public class TeamInvitation {
    private int id;
    private int equipeId;
    private int userId;
    private String equipeNom;
    private String ownerNom;
    private String status; // invited, accepted, refused
    private Timestamp createdAt;

    public TeamInvitation() {}

    public TeamInvitation(int id, int equipeId, int userId, String equipeNom,
                          String ownerNom, String status, Timestamp createdAt) {
        this.id = id;
        this.equipeId = equipeId;
        this.userId = userId;
        this.equipeNom = equipeNom;
        this.ownerNom = ownerNom;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEquipeId() { return equipeId; }
    public void setEquipeId(int equipeId) { this.equipeId = equipeId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEquipeNom() { return equipeNom; }
    public void setEquipeNom(String equipeNom) { this.equipeNom = equipeNom; }

    public String getOwnerNom() { return ownerNom; }
    public void setOwnerNom(String ownerNom) { this.ownerNom = ownerNom; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
