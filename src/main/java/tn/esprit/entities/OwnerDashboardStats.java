package tn.esprit.entities;

public class OwnerDashboardStats {
    private int ownedTeamsCount;
    private int joinedTeamsCount;
    private int pendingRequestsCount;
    private int totalFinishedMatches;
    private int wins;
    private int draws;
    private int losses;
    private double winRate;
    private boolean hasFullTeam;
    private int totalMembers;
    private int totalMaxMembers;

    public int getOwnedTeamsCount() {
        return ownedTeamsCount;
    }

    public void setOwnedTeamsCount(int ownedTeamsCount) {
        this.ownedTeamsCount = ownedTeamsCount;
    }

    public int getJoinedTeamsCount() {
        return joinedTeamsCount;
    }

    public void setJoinedTeamsCount(int joinedTeamsCount) {
        this.joinedTeamsCount = joinedTeamsCount;
    }

    public int getPendingRequestsCount() {
        return pendingRequestsCount;
    }

    public void setPendingRequestsCount(int pendingRequestsCount) {
        this.pendingRequestsCount = pendingRequestsCount;
    }

    public int getTotalFinishedMatches() {
        return totalFinishedMatches;
    }

    public void setTotalFinishedMatches(int totalFinishedMatches) {
        this.totalFinishedMatches = totalFinishedMatches;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }

    public boolean isHasFullTeam() {
        return hasFullTeam;
    }

    public void setHasFullTeam(boolean hasFullTeam) {
        this.hasFullTeam = hasFullTeam;
    }

    public int getTotalMembers() {
        return totalMembers;
    }

    public void setTotalMembers(int totalMembers) {
        this.totalMembers = totalMembers;
    }

    public int getTotalMaxMembers() {
        return totalMaxMembers;
    }

    public void setTotalMaxMembers(int totalMaxMembers) {
        this.totalMaxMembers = totalMaxMembers;
    }
}