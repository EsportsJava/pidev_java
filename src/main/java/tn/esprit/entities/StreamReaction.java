package tn.esprit.entities;

public class StreamReaction {

    private int id;
    private String type;   // emoji
    private String username;
    private int streamId;

    public StreamReaction() {}

    public StreamReaction(String type, String username, int streamId) {
        this.type = type;
        this.username = username;
        this.streamId = streamId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }
}