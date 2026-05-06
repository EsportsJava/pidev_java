package tn.esprit.entities;

import java.sql.Timestamp;

public class VideoReaction {

    private int id;
    private int videoId;
    private String username;
    private String type; // ❤️ 😂 😮 😡
    private Timestamp createdAt;

    public VideoReaction() {}

    public VideoReaction(int id, int videoId, String username, String type, Timestamp createdAt) {
        this.id = id;
        this.videoId = videoId;
        this.username = username;
        this.type = type;
        this.createdAt = createdAt;
    }

    public VideoReaction(int videoId, String username, String type) {
        this.videoId = videoId;
        this.username = username;
        this.type = type;
    }

    public int getId() { return id; }

    public int getVideoId() { return videoId; }

    public String getUsername() { return username; }

    public String getType() { return type; }

    public Timestamp getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }

    public void setVideoId(int videoId) { this.videoId = videoId; }

    public void setUsername(String username) { this.username = username; }

    public void setType(String type) { this.type = type; }

    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}