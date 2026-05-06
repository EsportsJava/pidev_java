package tn.esprit.entities;

public class User {
    private int id;
    private String email;
    private String roles;
    private String password;
    private String nom;
    private boolean isActive;
    private String google2faSecret;
    private boolean is2faEnabled;
    private String googleOauthId;
    private String oauthProvider;
    private String profileImageUrl;
    private String faceEncoding;
    private boolean isFaceEnabled;

    public User() {
    }

    public User(int id, String email, String roles, String password, String nom, boolean isActive,
                String google2faSecret, boolean is2faEnabled, String googleOauthId, String oauthProvider,
                String profileImageUrl,
                String faceEncoding, boolean isFaceEnabled) {
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.nom = nom;
        this.isActive = isActive;
        this.google2faSecret = google2faSecret;
        this.is2faEnabled = is2faEnabled;
        this.googleOauthId = googleOauthId;
        this.oauthProvider = oauthProvider;
        this.profileImageUrl = profileImageUrl;
        this.faceEncoding = faceEncoding;
        this.isFaceEnabled = isFaceEnabled;
    }

    public User(String email, String roles, String password, String nom, boolean isActive,
                String google2faSecret, boolean is2faEnabled, String googleOauthId, String oauthProvider,
                String profileImageUrl,
                String faceEncoding, boolean isFaceEnabled) {
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.nom = nom;
        this.isActive = isActive;
        this.google2faSecret = google2faSecret;
        this.is2faEnabled = is2faEnabled;
        this.googleOauthId = googleOauthId;
        this.oauthProvider = oauthProvider;
        this.profileImageUrl = profileImageUrl;
        this.faceEncoding = faceEncoding;
        this.isFaceEnabled = isFaceEnabled;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getGoogle2faSecret() {
        return google2faSecret;
    }

    public void setGoogle2faSecret(String google2faSecret) {
        this.google2faSecret = google2faSecret;
    }

    public boolean isIs2faEnabled() {
        return is2faEnabled;
    }

    public void setIs2faEnabled(boolean is2faEnabled) {
        this.is2faEnabled = is2faEnabled;
    }

    public String getGoogleOauthId() {
        return googleOauthId;
    }

    public void setGoogleOauthId(String googleOauthId) {
        this.googleOauthId = googleOauthId;
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getFaceEncoding() {
        return faceEncoding;
    }

    public void setFaceEncoding(String faceEncoding) {
        this.faceEncoding = faceEncoding;
    }

    public boolean isFaceEnabled() {
        return isFaceEnabled;
    }

    public void setFaceEnabled(boolean faceEnabled) {
        isFaceEnabled = faceEnabled;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", nom='" + nom + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
