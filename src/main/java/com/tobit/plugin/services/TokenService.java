package com.tobit.plugin.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.tobit.plugin.models.data.ApiResponse;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.time.Instant;

@Service(Service.Level.PROJECT)
public final class TokenService {
    private final ApiService apiService;
    private final StorageService storageService;
    private String currentToken = "";
    private String currentTobitDevToken = "";
    private final List<TokenChangeListener> listeners = new ArrayList<>();

    private static final int DEFAULT_LOCATION_ID = 378;
    private static final String RENEW_TOKEN_KEY = "renewToken";
    private static final String RENEW_TOKEN_EXPIRY_KEY = "renewTokenExpiry";
    private static final String ACCESS_TOKEN_KEY = "accessToken";
    private static final String TOKEN_EXPIRY_KEY = "tokenExpiry";
    private static final int TOKEN_TYPE_RENEW = 4;
    private static final int TOKEN_TYPE_ACCESS = 1;
    private static final String REMOTE_LOGIN_SITE_ID = "60021-08989";
    private static final String TOBIT_DEV_SITE_ID = "70266-09943";
    private static final long REMOTE_LOGIN_TIMEOUT = 5 * 60 * 1000L; // 5 minutes
    private static final long RENEW_TOKEN_RENEWAL_BUFFER = 7 * 24 * 60 * 60 * 1000L; // 7 days before expiry

    public TokenService(Project project) {
        this.apiService = new ApiService();
        this.storageService = new StorageService(project);

        // Create internal listener to update dev token when regular token changes
        TokenChangeListener internalTokenListener = newToken -> {
            if (!newToken.isEmpty()) {
                updateTobitDevToken();
            }
        };

        // Register internal listener
        addTokenChangeListener(internalTokenListener);
    }

    public static TokenService getInstance(Project project) {
        return ServiceManager.getService(project, TokenService.class);
    }

    public String getToken() {
        if (currentToken.isEmpty()) {
            loadAccessToken();
        }
        return currentToken;
    }

    public String getTobitDevToken() {
        return currentTobitDevToken;
    }

    public interface TokenChangeListener {
        void onTokenChanged(String newToken);
    }

    public void addTokenChangeListener(TokenChangeListener listener) {
        listeners.add(listener);
    }

    public void removeTokenChangeListener(TokenChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (TokenChangeListener listener : listeners) {
            listener.onTokenChanged(currentToken);
        }
    }

    private void updateTobitDevToken() {
        try {
            String devToken = switchSite(TOBIT_DEV_SITE_ID);
            if (devToken != null) {
                currentTobitDevToken = devToken;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean shouldRenewRenewToken() {
        String expiryStr = storageService.getValue(RENEW_TOKEN_EXPIRY_KEY);
        if (expiryStr == null || expiryStr.isEmpty()) return true;

        try {
            // Parse the ISO-8601 date format
            Instant expiryInstant = Instant.parse(expiryStr);
            Instant renewalThreshold = Instant.now().plusMillis(RENEW_TOKEN_RENEWAL_BUFFER);

            // If expiry is before our renewal threshold, we should renew
            return expiryInstant.isBefore(renewalThreshold);
        } catch (Exception ex) {
            ex.printStackTrace();
            return true; // If we can't parse the date, better safe than sorry
        }
    }

    public void ensureValidRenewToken() {
        if (shouldRenewRenewToken()) {
            renewRenewToken();
        }
    }

    public void renewRenewToken() {
        String currentRenewToken = storageService.getValue(RENEW_TOKEN_KEY);
        if (currentRenewToken == null || currentRenewToken.isEmpty()) return;

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("locationId", DEFAULT_LOCATION_ID);
            requestBody.put("tokenType", TOKEN_TYPE_RENEW);

            ApiResponse response = apiService.postRequest(
                    "https://auth.tobit.com/v2/token",
                    requestBody.toString(),
                    java.util.Collections.singletonMap("Authorization", "Bearer " + currentRenewToken)
            );

            if (response.isSuccess()) {
                JSONObject jsonResponse = new JSONObject(response.data());
                String newRenewToken = jsonResponse.optString("token", "");
                String expiryDate = jsonResponse.optString("expires", "");

                if (!newRenewToken.isEmpty()) {
                    storageService.setValue(RENEW_TOKEN_KEY, newRenewToken);
                    // Store the expiry date for the renew token
                    storageService.setValue(RENEW_TOKEN_EXPIRY_KEY, expiryDate);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean getAccessToken(String renewToken) {
        if (renewToken.isEmpty()) return false;

        // Check if we need to renew the renewToken before using it
        ensureValidRenewToken();

        // Get the possibly updated renewToken
        renewToken = storageService.getValue(RENEW_TOKEN_KEY);
        if (renewToken == null || renewToken.isEmpty()) return false;

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("locationId", DEFAULT_LOCATION_ID);
            requestBody.put("tokenType", TOKEN_TYPE_ACCESS);

            ApiResponse response = apiService.postRequest(
                    "https://auth.tobit.com/v2/token",
                    requestBody.toString(),
                    java.util.Collections.singletonMap("Authorization", "Bearer " + renewToken)
            );

            if (response.isSuccess()) {
                JSONObject jsonResponse = new JSONObject(response.data());
                currentToken = jsonResponse.optString("token", "");
                long expiryMs = System.currentTimeMillis() + (jsonResponse.optInt("expiresIn", 3600) * 1000L);

                storageService.setValue(ACCESS_TOKEN_KEY, currentToken);
                storageService.setValue(TOKEN_EXPIRY_KEY, String.valueOf(expiryMs));

                notifyListeners();
                return !currentToken.isEmpty();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private boolean getAccessToken() {
        String renewToken = storageService.getValue(RENEW_TOKEN_KEY);
        if (renewToken == null) renewToken = "";
        return getAccessToken(renewToken);
    }

    @Nullable
    public String switchSite(String siteId) {
        if (currentToken.isEmpty()) {
            if (!loadAccessToken()) return null;
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("siteId", siteId);

            ApiResponse response = apiService.postRequest(
                    "https://auth.tobit.com/v2/token",
                    requestBody.toString(),
                    java.util.Collections.singletonMap("Authorization", "Bearer " + currentToken)
            );

            if (response.isSuccess()) {
                JSONObject jsonResponse = new JSONObject(response.data());
                return jsonResponse.optString("token", "");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean remoteLogin() {
        try {
            // Get remote login code and secret
            ApiResponse codeResponse = apiService.getRequest("https://auth.chayns.net/v2/remotelogin?siteId=" + REMOTE_LOGIN_SITE_ID + "&uacs=");

            if (!codeResponse.isSuccess()) {
                return false;
            }

            JSONObject jsonResponse = new JSONObject(codeResponse.data());
            String code = jsonResponse.optString("code", "");
            String secret = jsonResponse.optString("secret", "");

            if (code.isEmpty() || secret.isEmpty()) {
                return false;
            }

            // Open browser for user authentication
            String url = "https://chayns.de?tappAction=cc&ccUrl=https://chayns.cc/login/" + code + "?m=-1";
            openInBrowser(url);

            // Wait for user authentication via WebSocket
            boolean authenticated = connectToWebSocketAndWaitForAuth(code);
            if (!authenticated) {
                return false;
            }

            // Exchange code and secret for renew token
            JSONObject requestBody = new JSONObject();
            requestBody.put("tokenType", TOKEN_TYPE_RENEW);

            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("Authorization", "Basic " + encodeBasicAuth(code, secret));
            headers.put("X-Authorization-Provider", "TobitRemote");
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/json");

            ApiResponse tokenResponse = apiService.postRequest(
                    "https://auth.chayns.net/v2/token",
                    requestBody.toString(),
                    headers
            );

            if (tokenResponse.isSuccess()) {
                JSONObject tokenJson = new JSONObject(tokenResponse.data());
                String renewToken = tokenJson.optString("token", "");
                String expiryDate = tokenJson.optString("expires", "");

                if (!renewToken.isEmpty()) {
                    storageService.setValue(RENEW_TOKEN_KEY, renewToken);
                    storageService.setValue(RENEW_TOKEN_EXPIRY_KEY, expiryDate);
                    return getAccessToken(renewToken);
                }
            }
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean connectToWebSocketAndWaitForAuth(String code) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        try {
            WebSocketClient client = new WebSocketClient(new URI("wss://websocket.tobit.com")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    JSONObject registerMessage = new JSONObject();
                    JSONObject data = new JSONObject();
                    JSONObject conditions = new JSONObject();

                    conditions.put("code", code);
                    data.put("application", "chayns_auth");
                    data.put("conditions", conditions);
                    registerMessage.put("topic", "register");
                    registerMessage.put("data", data);

                    send(registerMessage.toString());
                }

                @Override
                public void onMessage(String message) {
                    if (message != null) {
                        try {
                            JSONObject json = new JSONObject(message);
                            if ("remote_login".equals(json.optString("topic"))) {
                                success[0] = true;
                                latch.countDown();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    latch.countDown();
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                    latch.countDown();
                }
            };

            client.connect();

            // Wait up to 5 minutes for authentication
            latch.await(5, TimeUnit.MINUTES);
            client.close();
            return success[0];

        } catch (Exception ex) {
            ex.printStackTrace();
            latch.countDown();
            return false;
        }
    }

    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace();
        }
    }

    private boolean loadAccessToken() {
        String savedToken = storageService.getValue(ACCESS_TOKEN_KEY);
        String expiryStr = storageService.getValue(TOKEN_EXPIRY_KEY);

        if (savedToken == null) savedToken = "";
        if (expiryStr == null) expiryStr = "0";

        long expiry;
        try {
            expiry = Long.parseLong(expiryStr);
        } catch (NumberFormatException e) {
            expiry = 0;
        }

        // If token exists and is not expired, use it
        if (!savedToken.isEmpty() && System.currentTimeMillis() < expiry) {
            currentToken = savedToken;
            notifyListeners();
            return true;
        }

        // Otherwise get a new access token using the renew token
        return getAccessToken();
    }

    public boolean hasRenewToken() {
        String token = storageService.getValue(RENEW_TOKEN_KEY);
        return token != null && !token.isEmpty();
    }

    private String encodeBasicAuth(String username, String password) {
        return Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    public void logout() {
        // Clear stored tokens
        storageService.setValue(RENEW_TOKEN_KEY, "");
        storageService.setValue(RENEW_TOKEN_EXPIRY_KEY, "");
        storageService.setValue(ACCESS_TOKEN_KEY, "");
        storageService.setValue(TOKEN_EXPIRY_KEY, "");
        currentToken = "";
        currentTobitDevToken = "";
        notifyListeners();
    }
}