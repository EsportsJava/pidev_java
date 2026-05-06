package tn.esprit.services;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloudinaryService {

    private static final String PROPERTIES_FILE = "cloudinary.properties";
    private static final Pattern CLOUDINARY_URL_PATTERN =
            Pattern.compile("^cloudinary://([^:]+):([^@]+)@(.+)$");
    private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(60))
            .build();
    private final Properties properties = loadProperties();

    public boolean isConfigured() {
        return !getCloudName().isBlank() && !getApiKey().isBlank() && !getApiSecret().isBlank();
    }

    public String getConfigurationError() {
        return "Cloudinary is not configured. Set CLOUDINARY_URL, or CLOUDINARY_CLOUD_NAME/CLOUDINARY_API_KEY/CLOUDINARY_API_SECRET, or create cloudinary.properties.";
    }

    public String uploadProfileImage(File file, int userId) throws IOException {
        if (!isConfigured()) {
            throw new IOException(getConfigurationError());
        }
        if (file == null || !file.exists()) {
            throw new IOException("Selected image file does not exist.");
        }

        String folder = getFolder();
        String publicId = "user_" + userId + "_" + System.currentTimeMillis();
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        file.getName(),
                        RequestBody.create(Files.readAllBytes(file.toPath()), OCTET_STREAM)
                )
                .addFormDataPart("public_id", publicId);

        if (!folder.isBlank()) {
            bodyBuilder.addFormDataPart("folder", folder);
        }

        Request request = new Request.Builder()
                .url("https://api.cloudinary.com/v1_1/" + getCloudName() + "/image/upload")
                .addHeader("Authorization", Credentials.basic(getApiKey(), getApiSecret()))
                .post(bodyBuilder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException(extractError(response.code(), responseBody));
            }

            JSONObject payload = new JSONObject(responseBody);
            String secureUrl = payload.optString("secure_url", "");
            if (secureUrl.isBlank()) {
                throw new IOException("Cloudinary upload succeeded but returned no secure_url.");
            }
            return secureUrl;
        }
    }

    private String getCloudName() {
        CloudinaryUrlParts parts = parseCloudinaryUrl();
        if (parts != null) {
            return parts.cloudName();
        }
        return readConfig("CLOUDINARY_CLOUD_NAME");
    }

    private String getApiKey() {
        CloudinaryUrlParts parts = parseCloudinaryUrl();
        if (parts != null) {
            return parts.apiKey();
        }
        return readConfig("CLOUDINARY_API_KEY");
    }

    private String getApiSecret() {
        CloudinaryUrlParts parts = parseCloudinaryUrl();
        if (parts != null) {
            return parts.apiSecret();
        }
        return readConfig("CLOUDINARY_API_SECRET");
    }

    private String getFolder() {
        return readConfig("CLOUDINARY_FOLDER");
    }

    private String readConfig(String key) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = System.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String fileValue = properties.getProperty(key);
        return fileValue == null ? "" : fileValue.trim();
    }

    private CloudinaryUrlParts parseCloudinaryUrl() {
        String cloudinaryUrl = readRawConfig("CLOUDINARY_URL");
        if (cloudinaryUrl.isBlank()) {
            return null;
        }

        Matcher matcher = CLOUDINARY_URL_PATTERN.matcher(cloudinaryUrl.trim());
        if (!matcher.matches()) {
            return null;
        }

        return new CloudinaryUrlParts(
                matcher.group(1).trim(),
                matcher.group(2).trim(),
                matcher.group(3).trim()
        );
    }

    private String readRawConfig(String key) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = System.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String fileValue = properties.getProperty(key);
        return fileValue == null ? "" : fileValue.trim();
    }

    private Properties loadProperties() {
        Properties loaded = new Properties();
        try (var input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                loaded.load(input);
            }
        } catch (IOException ignored) {
        }

        File rootFile = new File(PROPERTIES_FILE);
        if (rootFile.exists()) {
            try (var input = Files.newInputStream(rootFile.toPath())) {
                loaded.load(input);
            } catch (IOException ignored) {
            }
        }

        return loaded;
    }

    private String extractError(int statusCode, String responseBody) {
        try {
            JSONObject payload = new JSONObject(responseBody);
            JSONObject error = payload.optJSONObject("error");
            if (error != null) {
                String message = error.optString("message", "");
                if (!message.isBlank()) {
                    return "Cloudinary upload failed: " + message;
                }
            }
        } catch (Exception ignored) {
        }

        return "Cloudinary upload failed with status " + statusCode + ".";
    }

    private record CloudinaryUrlParts(String apiKey, String apiSecret, String cloudName) {}
}
