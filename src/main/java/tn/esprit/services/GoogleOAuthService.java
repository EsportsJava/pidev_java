package tn.esprit.services;

import tn.esprit.entities.User;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleOAuthService {
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final HttpClient httpClient;
    private final String clientId;
    private final String clientSecret;
    private String redirectUri;

    public GoogleOAuthService(String clientId, String clientSecret) {
        this.httpClient = HttpClient.newHttpClient();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        // redirectUri will be set dynamically when server starts
    }

    public String getAuthorizationUrl() {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("redirect_uri", redirectUri);
        params.put("scope", "openid email profile");
        params.put("response_type", "code");
        params.put("access_type", "offline");

        StringBuilder url = new StringBuilder(AUTH_URL + "?");
        params.forEach((key, value) -> {
            if (url.length() > AUTH_URL.length() + 1) url.append("&");
            url.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });

        return url.toString();
    }

    public User authenticateAndGetUser(String authorizationCode) throws IOException, InterruptedException {
        // Exchange code for access token
        String tokenResponse = exchangeCodeForToken(authorizationCode);
        String accessToken = extractJsonValue(tokenResponse, "access_token");

        if (accessToken == null) {
            throw new IOException("Failed to extract access token from response: " + tokenResponse);
        }

        // Get user info
        String userInfoResponse = getUserInfo(accessToken);

        String email = extractJsonValue(userInfoResponse, "email");
        String name = extractJsonValue(userInfoResponse, "name");
        String id = extractJsonValue(userInfoResponse, "id");

        if (email == null || name == null || id == null) {
            throw new IOException("Failed to extract user info from response: " + userInfoResponse);
        }

        User user = new User();
        user.setEmail(email);
        user.setNom(name);
        user.setGoogleOauthId(id);
        user.setOauthProvider("google");
        user.setRoles("[\"ROLE_USER\"]");
        user.setActive(true);

        return user;
    }

    private String exchangeCodeForToken(String code) throws IOException, InterruptedException {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("code", code);
        params.put("grant_type", "authorization_code");
        params.put("redirect_uri", redirectUri);

        String formData = buildFormData(params);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String getUserInfo(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USERINFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String buildFormData(Map<String, String> params) {
        StringBuilder formData = new StringBuilder();
        params.forEach((key, value) -> {
            if (formData.length() > 0) formData.append("&");
            formData.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return formData.toString();
    }

    private String extractJsonValue(String json, String key) {
        // Simple regex to extract JSON string values, handling whitespace
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public CompletableFuture<User> authenticateAndGetUserAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return authenticateAndGetUser();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public User authenticateAndGetUser() throws Exception {
        // Try port 80 first (matches Google OAuth config), fallback to 8888
        int[] portsToTry = {80, 8888};
        ServerSocket serverSocket = null;
        
        for (int port : portsToTry) {
            try {
                serverSocket = new ServerSocket(port);
                this.redirectUri = "http://localhost" + (port == 80 ? "" : ":" + port);
                System.out.println("OAuth server listening on port " + port + " with redirect URI: " + this.redirectUri);
                break;
            } catch (Exception e) {
                System.out.println("Could not bind to port " + port + ": " + e.getMessage());
                if (port == portsToTry[portsToTry.length - 1]) {
                    throw new RuntimeException("Could not start OAuth server on any port");
                }
            }
        }
        
        serverSocket.setSoTimeout(300000); // 5 minutes timeout

        try {
            // Open browser AFTER server is ready
            String authUrl = getAuthorizationUrl();
            System.out.println("Opening browser with URL: " + authUrl);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(authUrl));
            } else {
                System.out.println("Please open this URL in your browser: " + authUrl);
            }

            // Wait for callback
            Socket clientSocket = serverSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream());

            String line;
            String authorizationCode = null;
            String requestLine = null;
            boolean headersStarted = false;
            
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);
                
                // Check for GET request line
                if (line.startsWith("GET ")) {
                    requestLine = line;
                    System.out.println("Found GET request: " + requestLine);
                    String path = line.split(" ")[1];
                    if (path.startsWith("/?code=") || path.contains("code=")) {
                        authorizationCode = extractCodeFromQuery(path);
                        System.out.println("Extracted code: " + (authorizationCode != null ? "YES" : "NO"));
                        break;
                    }
                }
                
                // Stop reading after empty line (end of headers)
                if (line.trim().isEmpty()) {
                    if (headersStarted) {
                        break;
                    }
                    headersStarted = true;
                }
            }

            // Send response
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println();
            out.println("<html><body><h1>Authentication successful! You can close this window.</h1></body></html>");
            out.flush();

            clientSocket.close();

            if (authorizationCode != null) {
                System.out.println("Authorization code received, proceeding with token exchange...");
                return authenticateAndGetUser(authorizationCode);
            } else {
                System.err.println("No authorization code received from callback");
                if (requestLine != null) {
                    System.err.println("Request line was: " + requestLine);
                }
                throw new RuntimeException("No authorization code received");
            }

        } finally {
            serverSocket.close();
        }
    }

    private String extractCodeFromQuery(String query) {
        try {
            // query looks like /?code=...&scope=... or /callback?code=...&scope=...
            // Find the code parameter
            int codeIndex = query.indexOf("code=");
            if (codeIndex != -1) {
                int start = codeIndex + 5;
                int end = query.indexOf("&", start);
                if (end == -1) end = query.length();
                String code = query.substring(start, end);
                // URL decode the code
                code = java.net.URLDecoder.decode(code, StandardCharsets.UTF_8);
                System.out.println("Extracted authorization code: " + code.substring(0, Math.min(20, code.length())) + "...");
                return code;
            }
        } catch (Exception e) {
            System.err.println("Error extracting code from query: " + e.getMessage());
        }
        return null;
    }
}