// src/main/java/com/tobit/plugin/service/ChaynsApiService.java
package com.tobit.plugin.services;

import com.tobit.plugin.models.data.ApiResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ApiService {

    public ApiResponse getRequest(String url) {
        return getRequest(url, Collections.emptyMap());
    }

    public ApiResponse getRequest(String url, Map<String, String> headers) {
        return makeRequest("GET", url, null, headers);
    }

    public ApiResponse postRequest(String url, String body) {
        return postRequest(url, body, Collections.emptyMap());
    }

    public ApiResponse postRequest(String url, String body, Map<String, String> headers) {
        Map<String, String> contentTypeHeaders = new HashMap<>(headers);
        if (!headers.containsKey("Content-Type")) {
            contentTypeHeaders.put("Content-Type", "application/json");
        }
        return makeRequest("POST", url, body, contentTypeHeaders);
    }

    private ApiResponse makeRequest(String method, String url, String body, Map<String, String> headers) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);

            // Add headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Add body for POST requests
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8")) {
                    writer.write(body);
                    writer.flush();
                }
            }

            // Handle response
            int statusCode = connection.getResponseCode();

            BufferedReader reader;
            if (statusCode < 400) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }

            StringBuilder responseText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseText.append(line);
            }
            reader.close();

            return new ApiResponse(responseText.toString(), statusCode);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse("", 500);
        }
    }
}