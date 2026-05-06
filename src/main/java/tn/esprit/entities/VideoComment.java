package tn.esprit.entities;

import java.sql.Timestamp;

public class VideoComment {

    private int id;
    private int videoId;
    private String username;
    private String body;
    private Timestamp createdAt;

    public VideoComment() {
    }

    public VideoComment(int id, int videoId, String username, String body, Timestamp createdAt) {
        this.id = id;
        this.videoId = videoId;
        this.username = username;
        this.body = body;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVideoId() {
        return videoId;
    }

    public void setVideoId(int videoId) {
        this.videoId = videoId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
