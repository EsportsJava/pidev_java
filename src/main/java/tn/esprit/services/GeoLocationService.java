package tn.esprit.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GeoLocationService {

    // 🔑 Remplace par TA clé API IPGeolocation
    private static final String API_KEY = "a8a2f19d317044d5bfb2f96e04c48f40";
    private static final String BASE_URL = "https://api.ipgeolocation.io/ipgeo";

    /**
     * Récupère les informations de localisation à partir d'une adresse IP
     * @param ip Adresse IP (ou "api" pour utiliser l'IP du visiteur)
     * @return Objet Location contenant pays, drapeau, etc.
     */
    public Location getLocation(String ip) {
        try {
            String urlString;
            if (ip == null || ip.isEmpty() || ip.equals("api")) {
                urlString = BASE_URL + "?apiKey=" + API_KEY;
            } else {
                urlString = BASE_URL + "?apiKey=" + API_KEY + "&ip=" + ip;
            }

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Erreur API IPGeolocation: " + responseCode);
                return new Location("Inconnu", "🏳️", "XX");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

            Location location = new Location();
            location.country = json.has("country_name") ? json.get("country_name").getAsString() : "Inconnu";
            location.countryCode = json.has("country_code2") ? json.get("country_code2").getAsString() : "XX";
            location.flag = getFlagEmoji(location.countryCode);
            location.city = json.has("city") ? json.get("city").getAsString() : "";
            location.isp = json.has("isp") ? json.get("isp").getAsString() : "";

            return location;

        } catch (Exception e) {
            e.printStackTrace();
            return new Location("Inconnu", "🏳️", "XX");
        }
    }

    /**
     * Convertit un code pays en emoji drapeau
     */
    private String getFlagEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) return "🏳️";

        // Convertir les lettres en emoji (offset pour les drapeaux)
        int firstLetter = countryCode.charAt(0) - 'A' + 0x1F1E6;
        int secondLetter = countryCode.charAt(1) - 'A' + 0x1F1E6;

        return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
    }

    public static class Location {
        public String country;
        public String flag;
        public String countryCode;
        public String city;
        public String isp;

        public Location() {}

        public Location(String country, String flag, String countryCode) {
            this.country = country;
            this.flag = flag;
            this.countryCode = countryCode;
        }

        public String getDisplayText() {
            return flag + " " + country;
        }
    }
}