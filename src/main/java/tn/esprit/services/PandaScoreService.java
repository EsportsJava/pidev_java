package tn.esprit.services;

import tn.esprit.entities.Equipe;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service API REST externe — ESPN (publique, sans token).
 * URL: https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams
 */
public class PandaScoreService {

    private static final String API_URL =
            "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams";

    private final ServiceEquipe serviceEquipe = new ServiceEquipe();

    public List<Equipe> fetchTeamsFromApi(int count) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("ESPN API erreur HTTP " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();

        JSONObject root = new JSONObject(response.toString());
        JSONArray sports = root.getJSONArray("sports");
        JSONArray leagues = sports.getJSONObject(0).getJSONArray("leagues");
        JSONArray teamsArray = leagues.getJSONObject(0).getJSONArray("teams");

        List<Equipe> teams = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < teamsArray.length(); i++) {
            indices.add(i);
        }
        Collections.shuffle(indices);

        for (int idx = 0; idx < Math.min(count, indices.size()); idx++) {
            JSONObject wrapper = teamsArray.getJSONObject(indices.get(idx));
            JSONObject teamObj = wrapper.getJSONObject("team");
            String name = teamObj.optString("displayName", "Team " + idx);
            String logoUrl = "";
            if (teamObj.has("logos") && teamObj.getJSONArray("logos").length() > 0) {
                logoUrl = teamObj.getJSONArray("logos").getJSONObject(0).optString("href", "");
            }
            Equipe equipe = new Equipe(name, 5, logoUrl);
            teams.add(equipe);
        }
        return teams;
    }

    public int generateTeamsFromApi(int count) throws Exception {
        List<Equipe> apiTeams = fetchTeamsFromApi(count);
        int created = 0;
        for (Equipe team : apiTeams) {
            try {
                serviceEquipe.ajouter(team);
                created++;
            } catch (SQLException e) {
                System.out.println("Skip: " + e.getMessage());
            }
        }
        return created;
    }
}
