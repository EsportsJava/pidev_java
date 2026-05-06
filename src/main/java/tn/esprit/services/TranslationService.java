package tn.esprit.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class TranslationService {

    private static final String API_KEY = "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";
    private static final String URL     = "https://api.groq.com/openai/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    public String translate(String text, String targetLanguage) {
        try {
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", "Translate the following text to " + targetLanguage +
                    ". Return ONLY the translated text, no explanations:\n\n" + text);

            JSONObject body = new JSONObject();
            body.put("model", "llama-3.3-70b-versatile");
            body.put("messages", new JSONArray().put(message));
            body.put("temperature", 0.3);
            body.put("max_tokens", 1024);

            Request request = new Request.Builder()
                    .url(URL)
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string() : "null";

                // ✅ Ces lignes sont déjà là
                System.out.println("📥 Status : " + response.code());
                System.out.println("📥 Réponse : " + responseBody);

                if (!response.isSuccessful()) {
                    System.out.println("❌ Erreur : " + responseBody);
                    return text;
                }


                JSONObject json = new JSONObject(responseBody);
                return json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim();
            }
        } catch (Exception e) {
            System.out.println("❌ Exception : " + e.getMessage());
            e.printStackTrace();
            return text;

        }

    }
}