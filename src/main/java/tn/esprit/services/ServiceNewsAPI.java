package tn.esprit.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ServiceNewsAPI {

    private static final String API_KEY = "ba957bbf292142c3b72e04db18c0bb4f";
    private static final String BASE_URL = "https://newsapi.org/v2/everything";

    public List<NewsArticle> getRelatedNews(String blogTitle) {
        List<NewsArticle> articles = new ArrayList<>();

        try {
            // 🔥 FORCER la recherche sur l'e-sport uniquement
            String searchQuery = "esport OR gaming OR tournament OR esports OR \"competitive gaming\"";

            // Exclure les catégories non liées à l'e-sport
            String excludeQuery = "-cars -auto -newbalance -shoes -clothing -deals -sale";

            String fullQuery = searchQuery + " " + excludeQuery;
            String encodedQuery = URLEncoder.encode(fullQuery, "UTF-8");

            // Chercher dans les catégories sport/jeux vidéo
            String urlString = BASE_URL + "?q=" + encodedQuery +
                    "&sortBy=publishedAt&pageSize=5&language=fr&apiKey=" + API_KEY +
                    "&sources=bbc-news,ign,espn,gamespot,polygon";

            System.out.println("🔍 Recherche e-sport: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Erreur API: " + responseCode);
                return articles;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray articlesArray = jsonResponse.getAsJsonArray("articles");

            if (articlesArray != null) {
                for (int i = 0; i < articlesArray.size(); i++) {
                    JsonObject article = articlesArray.get(i).getAsJsonObject();

                    String title = article.get("title").getAsString().toLowerCase();

                    // 🔥 FILTRER : Garder uniquement les articles e-sport
                    boolean isEsport = title.contains("esport") || title.contains("gaming") ||
                            title.contains("tournament") || title.contains("esports") ||
                            title.contains("league of legends") || title.contains("lol") ||
                            title.contains("valorant") || title.contains("csgo") ||
                            title.contains("counter strike") || title.contains("dota") ||
                            title.contains("overwatch") || title.contains("fortnite") ||
                            title.contains("rocket league") || title.contains("fighting game");

                    if (isEsport) {
                        NewsArticle news = new NewsArticle();
                        news.setTitle(article.get("title").getAsString());

                        String description = "";
                        if (article.has("description") && !article.get("description").isJsonNull()) {
                            description = article.get("description").getAsString();
                            if (description.length() > 150) {
                                description = description.substring(0, 150) + "...";
                            }
                        } else {
                            description = "Lire l'article complet...";
                        }
                        news.setDescription(description);

                        String source = "";
                        if (article.has("source") && article.getAsJsonObject("source").has("name")) {
                            source = article.getAsJsonObject("source").get("name").getAsString();
                        }
                        news.setSource(source);

                        news.setUrl(article.get("url").getAsString());
                        news.setPublishedAt(article.get("publishedAt").getAsString());

                        articles.add(news);

                        if (articles.size() >= 3) break; // On prend 3 articles max
                    }
                }
            }

            // 🔥 Si pas d'articles e-sport, chercher avec des mots-clés plus larges
            if (articles.isEmpty()) {
                articles = getFallbackEsportNews();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return articles;
    }

    private List<NewsArticle> getFallbackEsportNews() {
        List<NewsArticle> articles = new ArrayList<>();
        try {
            String urlString = BASE_URL + "?q=esports+OR+gaming+tournament&pageSize=3&language=en&apiKey=" + API_KEY;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray articlesArray = jsonResponse.getAsJsonArray("articles");

            if (articlesArray != null) {
                for (int i = 0; i < Math.min(articlesArray.size(), 3); i++) {
                    JsonObject article = articlesArray.get(i).getAsJsonObject();
                    NewsArticle news = new NewsArticle();
                    news.setTitle(article.get("title").getAsString());
                    news.setDescription("Actualité e-sport récente...");
                    news.setUrl(article.get("url").getAsString());
                    articles.add(news);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return articles;
    }

    public static class NewsArticle {
        private String title;
        private String description;
        private String source;
        private String url;
        private String publishedAt;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

        public String getFormattedDate() {
            try {
                String dateStr = publishedAt.substring(0, 10);
                String[] parts = dateStr.split("-");
                return parts[2] + "/" + parts[1] + "/" + parts[0];
            } catch (Exception e) {
                return "";
            }
        }
    }
}