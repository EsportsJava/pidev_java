package tn.esprit.entities;

import java.sql.Timestamp;

public class Comment {
    private int id;
    private int blogId;
    private int userId;
    private String content;
    private Timestamp createdAt;

    // Champs additionnels pour l'affichage
    private String userName;
    private String blogTitle;

    // Champs pour la géolocalisation (optionnel)
    private String userCountry;
    private String userCountryCode;
    private String userFlag;

    // Constructeurs
    public Comment() {}

    public Comment(int blogId, int userId, String content) {
        this.blogId = blogId;
        this.userId = userId;
        this.content = content;
    }

    // ========== GETTERS ==========
    public int getId() { return id; }
    public int getBlogId() { return blogId; }
    public int getUserId() { return userId; }
    public String getContent() { return content; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getUserName() { return userName; }
    public String getBlogTitle() { return blogTitle; }

    // Géolocalisation
    public String getUserCountry() { return userCountry; }
    public String getUserCountryCode() { return userCountryCode; }
    public String getUserFlag() { return userFlag; }

    // ========== SETTERS ==========
    public void setId(int id) { this.id = id; }
    public void setBlogId(int blogId) { this.blogId = blogId; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setBlogTitle(String blogTitle) { this.blogTitle = blogTitle; }

    // Géolocalisation
    public void setUserCountry(String userCountry) { this.userCountry = userCountry; }
    public void setUserCountryCode(String userCountryCode) { this.userCountryCode = userCountryCode; }
    public void setUserFlag(String userFlag) { this.userFlag = userFlag; }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", blogId=" + blogId +
                ", userName='" + userName + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
