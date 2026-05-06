package tn.esprit.entities;

import java.sql.Timestamp;

public class Blog {

    private int id;
    private String title;
    private String content;
    private Timestamp createdAt;
    private String category;
    private String imageName;
    private int commentCount;

    public Blog() {}

    public Blog(String title, String content, String category, String imageName) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.imageName = imageName;
    }

    public Blog(int id, String title, String content, Timestamp createdAt,
                String category, String imageName, int commentCount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.category = category;
        this.imageName = imageName;
        this.commentCount = commentCount;
    }

    // getters / setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    @Override
    public String toString() {
        return "Blog{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", comments=" + commentCount +
                '}';
    }
}