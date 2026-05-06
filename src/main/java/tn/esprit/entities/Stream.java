package tn.esprit.entities;

public class Stream {

    private int id;
    private String url;
    private boolean active;

    // ✅ constructeur vide (OBLIGATOIRE)
    public Stream() {}

    // ✅ constructeur complet (OBLIGATOIRE pour ton service)
    public Stream(int id, String url, boolean active) {
        this.id = id;
        this.url = url;
        this.active = active;
    }

    public Stream(String url) {
        this.url = url;
        this.active = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}