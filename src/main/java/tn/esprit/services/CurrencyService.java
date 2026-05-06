package tn.esprit.services;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;

public class CurrencyService {

    private static final String API_KEY = "0392364189bde15ff56af6dc"; // ← ta clé
    private static final String API_URL = "https://v6.exchangerate-api.com/v6/";

    public double convertFromTND(double amount, String targetCurrency) {
        OkHttpClient client = new OkHttpClient();

        String url = API_URL + API_KEY + "/pair/TND/" + targetCurrency + "/" + amount;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                System.out.println("✅ Conversion réussie !");
                return json.getDouble("conversion_result");
            }
        } catch (IOException e) {
            System.out.println("❌ Erreur conversion : " + e.getMessage());
        }
        return -1;
    }
}