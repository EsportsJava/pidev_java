package tn.esprit.entities;

import java.sql.Timestamp;

public class StreamComment {

    private int id;
    private int streamId;
    private String username;
    private String body;
    private Timestamp createdAt;

    public StreamComment() {
    }

    public StreamComment(int id, int streamId, String username, String body, Timestamp createdAt) {
        this.id = id;
        this.streamId = streamId;
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

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
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
