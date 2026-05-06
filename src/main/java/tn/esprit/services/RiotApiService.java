package tn.esprit.services;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.utils.ApiKeys;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class RiotApiService {
    private static final String[] ACCOUNT_ROUTING_VALUES = {"europe", "americas", "asia", "sea"};

    private final String apiKey;
    private final String henrikApiKey;
    private final OkHttpClient client = new OkHttpClient();

    public RiotApiService(String apiKey) {
        this.apiKey = normalizeApiKey(apiKey);
        this.henrikApiKey = readHenrikApiKey();
    }

    private static String normalizeApiKey(String raw) {
        if (raw == null) {
            return "";
        }
        String k = raw.trim();
        if (k.length() >= 2 && ((k.startsWith("\"") && k.endsWith("\"")) || (k.startsWith("'") && k.endsWith("'")))) {
            k = k.substring(1, k.length() - 1).trim();
        }
        return k.replace('\u00A0', ' ').trim();
    }

    /**
     * Résout un compte Riot ID sur tous les clusters Account-V1 (404 sur un cluster ≠ compte inexistant).
     */
    public JSONObject getAccountByRiotId(String gameName, String tagLine) throws IOException {
        return resolveAccount(gameName, tagLine).json();
    }

    private AccountResolution resolveAccount(String gameName, String tagLine) throws IOException {
        String encodedName = URLEncoder.encode(gameName.trim(), StandardCharsets.UTF_8);
        String encodedTag = URLEncoder.encode(tagLine.trim(), StandardCharsets.UTF_8);
        String path = "/riot/account/v1/accounts/by-riot-id/" + encodedName + "/" + encodedTag;

        for (String route : ACCOUNT_ROUTING_VALUES) {
            String url = "https://" + route + ".api.riotgames.com" + path;
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-Riot-Token", apiKey)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                int code = response.code();
                String body = response.body() != null ? response.body().string() : "";
                if (code == 200) {
                    return new AccountResolution(route, new JSONObject(body));
                }
                if (code != 404) {
                    throw new IOException(buildRiotErrorMessage(code, url, body));
                }
            }
        }

        throw new IOException(
                "Compte Riot introuvable. Verifiez le pseudo et le tag exactement comme dans le client "
                        + "(icone profil / ajouter ami : format Pseudo#TAG). "
                        + "Le tag n'est pas votre serveur de jeu : EUW/EUNE est souvent different du tag affiche apres #.");
    }

    public JSONObject fetchRecentStats(String jeuNom, String gameName, String tagLine) throws IOException {
        AccountResolution resolved = resolveAccount(gameName, tagLine);
        JSONObject account = resolved.json();
        String routing = resolved.routingValue();
        String puuid = account.optString("puuid", "");
        if (puuid.isEmpty()) {
            throw new IOException("PUUID Riot introuvable dans la reponse compte.");
        }

        JSONObject result = new JSONObject();
        result.put("jeuNom", jeuNom);
        result.put("puuid", puuid);
        result.put("riotRouting", routing);

        String normalized = jeuNom == null ? "" : jeuNom.toLowerCase();
        if (normalized.contains("valorant")) {
            result.put("riotGameName", account.optString("gameName", ""));
            result.put("riotTagLine", account.optString("tagLine", ""));
            JSONObject valData;
            try {
                valData = fetchValorantMatchlist(puuid, routing);
            } catch (IOException riotErr) {
                // Fallback: Riot /val/* can be blocked on some dev keys.
                valData = fetchValorantMatchlistFallback(
                        puuid,
                        account.optString("gameName", gameName),
                        account.optString("tagLine", tagLine),
                        routing,
                        riotErr);
            }
            result.put("source", "valorant");
            result.put("payload", valData);
            JSONArray history = valData.optJSONArray("history");
            if (history == null) {
                history = valData.optJSONArray("History");
            }
            int hLen = history == null ? 0 : history.length();
            result.put("valorantRecentMatchCount", hLen);
            return result;
        }

        result.put("riotGameName", account.optString("gameName", ""));
        result.put("riotTagLine", account.optString("tagLine", ""));

        String url = "https://" + routing + ".api.riotgames.com/lol/match/v5/matches/by-puuid/"
                + puuid + "/ids?start=0&count=5";
        JSONArray ids = getJsonArray(url);
        result.put("source", "lol");
        result.put("payload", ids);

        String platformId = inferLolPlatformFromMatchIds(ids);
        if (!platformId.isBlank()) {
            result.put("platformId", platformId);
            try {
                JSONObject summoner = fetchLolSummonerByPuuid(platformId, puuid);
                result.put("summonerLevel", summoner.optInt("summonerLevel", 0));
                String encSummonerId = summoner.optString("id", "");
                if (!encSummonerId.isBlank()) {
                    result.put("rankedEntries", fetchLolLeagueEntries(platformId, encSummonerId));
                } else {
                    result.put("rankedEntries", new JSONArray());
                }
            } catch (IOException e) {
                result.put("summonerLevel", 0);
                result.put("rankedEntries", new JSONArray());
            }
        } else {
            result.put("platformId", "");
            result.put("summonerLevel", 0);
            result.put("rankedEntries", new JSONArray());
        }

        JSONArray matchHistory = buildLolMatchHistory(routing, ids, puuid, 5);
        result.put("matchHistory", matchHistory);
        if (matchHistory.length() > 0) {
            result.put("lastMatch", matchHistory.getJSONObject(0));
        }
        return result;
    }

    private static String inferLolPlatformFromMatchIds(JSONArray matchIds) {
        if (matchIds == null || matchIds.length() == 0) {
            return "";
        }
        String mid = matchIds.optString(0, "");
        int u = mid.indexOf('_');
        if (u <= 0) {
            return "";
        }
        return mid.substring(0, u).toLowerCase(Locale.ROOT);
    }

    private JSONObject fetchLolSummonerByPuuid(String platformId, String puuid) throws IOException {
        String url = "https://" + platformId + ".api.riotgames.com/lol/summoner/v4/summoners/by-puuid/" + puuid;
        return getJson(url);
    }

    private JSONArray fetchLolLeagueEntries(String platformId, String encryptedSummonerId) throws IOException {
        String url = "https://" + platformId + ".api.riotgames.com/lol/league/v4/entries/by-summoner/"
                + URLEncoder.encode(encryptedSummonerId, StandardCharsets.UTF_8);
        return getJsonArray(url);
    }

    private JSONArray buildLolMatchHistory(String routing, JSONArray matchIds, String puuid, int max) {
        JSONArray out = new JSONArray();
        int n = Math.min(matchIds.length(), max);
        for (int i = 0; i < n; i++) {
            try {
                String mid = matchIds.getString(i);
                JSONObject row = extractLolParticipantSummary(routing, mid, puuid);
                if (row.length() > 1) {
                    out.put(row);
                }
            } catch (IOException ignored) {
                // partie ignorée si erreur réseau / match partiel
            }
        }
        return out;
    }

    private JSONObject extractLolParticipantSummary(String routing, String matchId, String playerPuuid) throws IOException {
        String url = "https://" + routing + ".api.riotgames.com/lol/match/v5/matches/" + matchId;
        JSONObject match = getJson(url);
        JSONObject info = match.optJSONObject("info");
        JSONObject preview = new JSONObject();
        preview.put("matchId", matchId);
        if (info == null) {
            return preview;
        }
        preview.put("gameDurationSeconds", info.optInt("gameDuration", 0));
        preview.put("queueId", info.optInt("queueId", 0));
        preview.put("gameMode", info.optString("gameMode", "CLASSIC"));
        JSONArray participants = info.optJSONArray("participants");
        if (participants == null) {
            return preview;
        }
        for (int i = 0; i < participants.length(); i++) {
            JSONObject p = participants.optJSONObject(i);
            if (p == null) {
                continue;
            }
            if (playerPuuid.equals(p.optString("puuid", ""))) {
                preview.put("championName", p.optString("championName", "?"));
                preview.put("kills", p.optInt("kills", 0));
                preview.put("deaths", p.optInt("deaths", 0));
                preview.put("assists", p.optInt("assists", 0));
                preview.put("win", p.optBoolean("win", false));
                preview.put("lane", p.optString("lane", ""));
                preview.put("teamPosition", p.optString("teamPosition", ""));
                break;
            }
        }
        return preview;
    }

    /**
     * Valorant utilise des hotes {@code eu.api}, {@code na.api}, etc., pas {@code europe.api}.
     */
    private JSONObject fetchValorantMatchlist(String puuid, String accountRouting) throws IOException {
        String[] platforms = valorantPlatformsOrder(accountRouting);
        IOException last404 = null;
        for (String platform : platforms) {
            String url = "https://" + platform + ".api.riotgames.com/val/match/v1/matchlists/by-puuid/" + puuid;
            try {
                return getJson(url);
            } catch (IOException e) {
                if (isNotFoundError(e)) {
                    last404 = e;
                    continue;
                }
                throw e;
            }
        }
        if (last404 != null) {
            throw new IOException(
                    "Historique Valorant introuvable sur les shards testes. "
                            + "Verifiez que ce compte a bien joue sur cette region ou reessayez plus tard.");
        }
        throw new IOException("Historique Valorant introuvable.");
    }

    private static String[] valorantPlatformsOrder(String accountRouting) {
        return switch (accountRouting) {
            case "europe" -> new String[]{"eu", "na", "latam", "br", "kr", "ap"};
            case "americas" -> new String[]{"na", "latam", "br", "eu", "kr", "ap"};
            case "asia" -> new String[]{"kr", "ap", "eu", "na", "latam", "br"};
            case "sea" -> new String[]{"ap", "kr", "eu", "na", "latam", "br"};
            default -> new String[]{"eu", "na", "kr", "ap", "latam", "br"};
        };
    }

    private static boolean isNotFoundError(IOException e) {
        String m = e.getMessage();
        return m != null && m.contains("(404)");
    }

    private static String readHenrikApiKey() {
        String fromClass = ApiKeys.HENRIK_API_KEY == null ? "" : ApiKeys.HENRIK_API_KEY.trim();
        if (!fromClass.isBlank()) {
            return fromClass;
        }
        String fromProperty = System.getProperty("HENRIK_API_KEY");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromEnv = System.getenv("HENRIK_API_KEY");
        return fromEnv == null ? "" : fromEnv.trim();
    }

    /**
     * Fallback provider (Henrik API) for Valorant when Riot /val/* is unavailable.
     */
    private JSONObject fetchValorantMatchlistFallback(
            String puuid,
            String gameName,
            String tagLine,
            String accountRouting,
            IOException originalError
    ) throws IOException {
        String encodedName = URLEncoder.encode(gameName == null ? "" : gameName.trim(), StandardCharsets.UTF_8);
        String encodedTag = URLEncoder.encode(tagLine == null ? "" : tagLine.trim(), StandardCharsets.UTF_8);
        String henrikPuuid = fetchHenrikAccountPuuid(gameName, tagLine);

        String[] regions = henrikRegionsOrder(accountRouting);
        IOException last = originalError;
        for (String region : regions) {
            String url = "https://api.henrikdev.xyz/valorant/v3/matches/"
                    + region + "/" + encodedName + "/" + encodedTag + "?size=5";
            try {
                JSONObject root = getJsonPublic(url);
                JSONArray data = root.optJSONArray("data");
                if (data == null) {
                    continue;
                }
                JSONArray history = new JSONArray();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject m = data.optJSONObject(i);
                    if (m == null) {
                        continue;
                    }
                    JSONObject row = extractHenrikValorantMatch(m, puuid, henrikPuuid, gameName, tagLine);
                    if (row.length() > 0) {
                        history.put(row);
                    }
                }
                JSONObject payload = new JSONObject();
                payload.put("provider", "henrik");
                payload.put("region", region);
                payload.put("history", history);
                return payload;
            } catch (IOException ex) {
                last = ex;
            }
        }
        throw new IOException("Impossible de recuperer les matchs Valorant (Riot + fallback). "
                + (last != null ? last.getMessage() : ""));
    }

    private String fetchHenrikAccountPuuid(String gameName, String tagLine) {
        try {
            String encodedName = URLEncoder.encode(gameName == null ? "" : gameName.trim(), StandardCharsets.UTF_8);
            String encodedTag = URLEncoder.encode(tagLine == null ? "" : tagLine.trim(), StandardCharsets.UTF_8);
            String accountUrl = "https://api.henrikdev.xyz/valorant/v1/account/" + encodedName + "/" + encodedTag;
            JSONObject root = getJsonPublic(accountUrl);
            JSONObject data = root.optJSONObject("data");
            return data != null ? data.optString("puuid", "") : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String[] henrikRegionsOrder(String accountRouting) {
        return switch (accountRouting) {
            case "europe" -> new String[]{"eu", "na", "ap", "kr", "latam", "br"};
            case "americas" -> new String[]{"na", "latam", "br", "eu", "ap", "kr"};
            case "asia" -> new String[]{"ap", "kr", "eu", "na", "latam", "br"};
            case "sea" -> new String[]{"ap", "kr", "eu", "na", "latam", "br"};
            default -> new String[]{"eu", "na", "ap", "kr", "latam", "br"};
        };
    }

    private static JSONObject extractHenrikValorantMatch(
            JSONObject match,
            String expectedRiotPuuid,
            String expectedHenrikPuuid,
            String gameName,
            String tagLine
    ) {
        JSONObject out = new JSONObject();
        JSONObject meta = match.optJSONObject("metadata");
        if (meta != null) {
            out.put("matchId", meta.optString("matchid", ""));
            out.put("map", meta.optString("map", ""));
            out.put("mode", meta.optString("mode", ""));
        }

        JSONObject players = match.optJSONObject("players");
        JSONArray all = players != null ? players.optJSONArray("all_players") : null;
        if (all == null) {
            return out;
        }

        String expectedName = normalizeIdentityPart(gameName);
        String expectedTag = normalizeIdentityPart(tagLine);
        JSONObject self = null;
        for (int i = 0; i < all.length(); i++) {
            JSONObject p = all.optJSONObject(i);
            if (p == null) {
                continue;
            }
            String puuid = p.optString("puuid", "");
            if (!expectedHenrikPuuid.isBlank() && expectedHenrikPuuid.equalsIgnoreCase(puuid)) {
                self = p;
                break;
            }
            if (!expectedRiotPuuid.isBlank() && expectedRiotPuuid.equalsIgnoreCase(puuid)) {
                self = p;
                break;
            }

            String name = normalizeIdentityPart(p.optString("name", ""));
            String tag = normalizeIdentityPart(p.optString("tag", ""));
            if (!expectedName.equals(name) || !expectedTag.equals(tag)) {
                continue;
            }
            self = p;
            break;
        }
        if (self == null) {
            return out;
        }

        out.put("character", self.optString("character", ""));
        out.put("team", self.optString("team", ""));
        JSONObject stats = self.optJSONObject("stats");
        if (stats != null) {
            out.put("kills", stats.optInt("kills", 0));
            out.put("deaths", stats.optInt("deaths", 0));
            out.put("assists", stats.optInt("assists", 0));
            out.put("score", stats.optInt("score", 0));
        }
        JSONObject teams = match.optJSONObject("teams");
        if (teams != null) {
            JSONObject team = teams.optJSONObject(self.optString("team", "").toLowerCase(Locale.ROOT));
            if (team != null) {
                out.put("win", team.optBoolean("has_won", false));
                out.put("roundsWon", team.optInt("rounds_won", 0));
                out.put("roundsLost", team.optInt("rounds_lost", 0));
            }
        }
        return out;
    }

    private static String normalizeIdentityPart(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private JSONObject getJson(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Riot-Token", apiKey)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IOException(buildRiotErrorMessage(response.code(), url, body));
            }
            return new JSONObject(response.body().string());
        }
    }

    private JSONArray getJsonArray(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Riot-Token", apiKey)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IOException(buildRiotErrorMessage(response.code(), url, body));
            }
            return new JSONArray(response.body().string());
        }
    }

    private JSONObject getJsonPublic(String url) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get();
        if (url.contains("api.henrikdev.xyz") && !henrikApiKey.isBlank()) {
            builder.addHeader("Authorization", henrikApiKey);
        }
        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                String body = response.body() != null ? response.body().string() : "";
                if (response.code() == 401 && url.contains("api.henrikdev.xyz")) {
                    throw new IOException("Fallback Valorant refuse (401) par Henrik API. "
                            + "Ajoutez une cle Henrik dans ApiKeys.HENRIK_API_KEY "
                            + "(ou -DHENRIK_API_KEY / variable d'environnement HENRIK_API_KEY).");
                }
                throw new IOException("API publique erreur (" + response.code() + ") pour " + url
                        + (body.isBlank() ? "" : " | details: " + body));
            }
            return new JSONObject(response.body().string());
        }
    }

    private String buildRiotErrorMessage(int statusCode, String url, String responseBody) {
        if (statusCode == 401 || statusCode == 403) {
            if (url != null && url.contains("/val/")) {
                return "Acces Valorant refuse (" + statusCode + "). "
                        + "Le endpoint VALORANT peut necessiter une cle/app approuvee par Riot "
                        + "(certaines cles Development n'ont pas acces a /val/*). "
                        + "Verifiez votre produit Riot dans le portail developpeur ou utilisez une API Valorant tierce.";
            }
            return "Cle Riot refusee (" + statusCode + "). Regenerer une cle sur developer.riotgames.com "
                    + "et la coller dans ApiKeys.RIOT_API_KEY (prioritaire), puis relancer l'app. "
                    + "Si ca persiste : supprimez RIOT_API_KEY des options VM du run (Run / Edit Configurations) "
                    + "et des variables Windows (setx). Les cles Development expirent souvent (~24h).";
        }
        if (statusCode == 404) {
            return "Ressource introuvable (404): " + url;
        }
        if (statusCode == 429) {
            return "Limite Riot atteinte (429). Attendez quelques secondes puis reessayez.";
        }
        String suffix = (responseBody == null || responseBody.isBlank()) ? "" : " | details: " + responseBody;
        return "Erreur Riot API (" + statusCode + ") pour " + url + suffix;
    }

    private record AccountResolution(String routingValue, JSONObject json) {}
}
