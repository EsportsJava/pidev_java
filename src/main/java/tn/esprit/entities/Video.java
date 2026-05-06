package tn.esprit.entities;

public class Video {
    private int commentsCount;
    private int reactionsCount;
    private int id;
    private String title;
    private String path;
    private String publicId;
    private String thumbnail;

    public Video() {}

    public Video(int id, String title, String path, String publicId, String thumbnail) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.publicId = publicId;
        this.thumbnail = thumbnail;
    }

    public Video(String title, String path, String publicId, String thumbnail) {
        this.title = title;
        this.path = path;
        this.publicId = publicId;
        this.thumbnail = thumbnail;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getPath() { return path; }
    public String getPublicId() { return publicId; }
    public String getThumbnail() { return thumbnail; }

    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setPath(String path) { this.path = path; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
}