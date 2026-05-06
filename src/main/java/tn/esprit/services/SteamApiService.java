package tn.esprit.services;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SteamApiService {
    private final String apiKey;
    private final OkHttpClient client = new OkHttpClient();

    public SteamApiService(String apiKey) {
        this.apiKey = apiKey;
    }

    public String findOfficialStoreUrl(String gameName) throws IOException {
        String encoded = URLEncoder.encode(gameName, StandardCharsets.UTF_8);
        String url = "https://store.steampowered.com/api/storesearch/?term=" + encoded + "&l=french&cc=tn";
        JSONObject data = getJson(url);
        JSONArray items = data.optJSONArray("items");
        if (items == null || items.length() == 0) {
            return "";
        }

        int appId = items.getJSONObject(0).optInt("id", 0);
        if (appId <= 0) {
            return "";
        }
        return "https://store.steampowered.com/app/" + appId + "/";
    }

    public JSONObject getPlayerSummary(String steamId64) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("Steam API key manquante.");
        }

        String url = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/"
                + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&steamids=" + URLEncoder.encode(steamId64, StandardCharsets.UTF_8);
        return getJson(url);
    }

    public JSONObject getCs2Stats(String steamId64) throws IOException {
        return getUserStatsForGame(730, steamId64);
    }

    public JSONObject getUserStatsForGame(int appId, String steamId64) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("Steam API key manquante.");
        }
        String url = "https://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/"
                + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&steamid=" + URLEncoder.encode(steamId64, StandardCharsets.UTF_8)
                + "&appid=" + appId;

        JSONObject root = getJson(url);
        JSONObject playerstats = root.optJSONObject("playerstats");
        if (playerstats == null) {
            return new JSONObject();
        }
        JSONArray stats = playerstats.optJSONArray("stats");
        JSONObject out = new JSONObject();
        if (stats == null) {
            return out;
        }
        for (int i = 0; i < stats.length(); i++) {
            JSONObject s = stats.optJSONObject(i);
            if (s == null) {
                continue;
            }
            String name = s.optString("name", "");
            if (name.isBlank()) {
                continue;
            }
            out.put(name, s.optInt("value", 0));
        }
        return out;
    }

    private JSONObject getJson(String url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Erreur Steam API (" + response.code() + ") pour " + url);
            }
            return new JSONObject(response.body().string());
        }
    }
}
