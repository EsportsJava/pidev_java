package tn.esprit.entities;

import java.sql.Timestamp;

public class MatchGame {
    private int id;
    private Timestamp dateMatch;
    private Integer scoreTeam1;
    private Integer scoreTeam2;
    private String statut;
    private int equipe1Id;
    private int equipe2Id;
    private int tournoiId;

    public MatchGame() {
    }

    public MatchGame(int id, Timestamp dateMatch, Integer scoreTeam1, Integer scoreTeam2, String statut,
                     int equipe1Id, int equipe2Id, int tournoiId) {
        this.id = id;
        this.dateMatch = dateMatch;
        this.scoreTeam1 = scoreTeam1;
        this.scoreTeam2 = scoreTeam2;
        this.statut = statut;
        this.equipe1Id = equipe1Id;
        this.equipe2Id = equipe2Id;
        this.tournoiId = tournoiId;
    }

    public MatchGame(Timestamp dateMatch, Integer scoreTeam1, Integer scoreTeam2, String statut,
                     int equipe1Id, int equipe2Id, int tournoiId) {
        this.dateMatch = dateMatch;
        this.scoreTeam1 = scoreTeam1;
        this.scoreTeam2 = scoreTeam2;
        this.statut = statut;
        this.equipe1Id = equipe1Id;
        this.equipe2Id = equipe2Id;
        this.tournoiId = tournoiId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Timestamp getDateMatch() {
        return dateMatch;
    }

    public void setDateMatch(Timestamp dateMatch) {
        this.dateMatch = dateMatch;
    }

    public Integer getScoreTeam1() {
        return scoreTeam1;
    }

    public void setScoreTeam1(Integer scoreTeam1) {
        this.scoreTeam1 = scoreTeam1;
    }

    public Integer getScoreTeam2() {
        return scoreTeam2;
    }

    public void setScoreTeam2(Integer scoreTeam2) {
        this.scoreTeam2 = scoreTeam2;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getEquipe1Id() {
        return equipe1Id;
    }

    public void setEquipe1Id(int equipe1Id) {
        this.equipe1Id = equipe1Id;
    }

    public int getEquipe2Id() {
        return equipe2Id;
    }

    public void setEquipe2Id(int equipe2Id) {
        this.equipe2Id = equipe2Id;
    }

    public int getTournoiId() {
        return tournoiId;
    }

    public void setTournoiId(int tournoiId) {
        this.tournoiId = tournoiId;
    }

    @Override
    public String toString() {
        return "MatchGame{" +
                "id=" + id +
                ", dateMatch=" + dateMatch +
                ", statut='" + statut + '\'' +
                ", equipe1Id=" + equipe1Id +
                ", equipe2Id=" + equipe2Id +
                ", tournoiId=" + tournoiId +
                '}';
    }
}
