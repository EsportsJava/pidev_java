package tn.esprit.services;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class HuggingFaceService {

    private static final String API_KEY = "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";

    public String generateDescription(String productName) {
        OkHttpClient client = new OkHttpClient();

        JSONObject body = new JSONObject();
        body.put("model", "llama-3.3-70b-versatile");

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", "Décris ce produit gaming en une phrase courte et attractive en français: " + productName);
        messages.put(userMessage);
        body.put("messages", messages);

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JSONObject json = new JSONObject(response.body().string());
                return json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }
        } catch (IOException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
        return null;
    }
}