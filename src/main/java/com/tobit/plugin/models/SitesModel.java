package com.tobit.plugin.models;

import com.intellij.openapi.project.Project;
import com.tobit.plugin.models.data.ApiResponse;
import com.tobit.plugin.models.data.LocationItem;
import com.tobit.plugin.services.ApiService;
import com.tobit.plugin.services.ChaynsCodesApiService;
import com.tobit.plugin.services.TokenService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SitesModel {
    private final ChaynsCodesApiService chaynsCodesApi;
    private final ApiService apiHelper;
    private final TokenService tokenService;

    // Data stores
    private List<LocationItem> savedSites = new ArrayList<>();
    private final List<JSONObject> searchResults = new ArrayList<>();

    // Listeners for data changes
    private final List<DataChangeListener> dataChangeListeners = new ArrayList<>();

    public interface DataChangeListener {
        void onDataChanged(List<LocationItem> savedSites, List<JSONObject> searchResults);
    }

    // Initialize data
    public SitesModel(Project project) {
        this.chaynsCodesApi = new ChaynsCodesApiService(project);
        this.apiHelper = new ApiService();
        this.tokenService = TokenService.getInstance(project);

        // Load saved sites on init
        loadSavedSites();
    }

    public void addDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.add(listener);
    }

    public void removeDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.remove(listener);
    }

    private void notifyDataChanged() {
        for (DataChangeListener listener : dataChangeListeners) {
            listener.onDataChanged(savedSites, searchResults);
        }
    }

    // Add to SitesModel.java
    public void searchMultipleSiteIds(List<String> siteIds) {
        searchResults.clear();

        if (tokenService.getToken().isEmpty()) {
            notifyDataChanged();
            return;
        }

        for (String siteId : siteIds) {
            String trimmedId = siteId.trim();
            if (!trimmedId.isEmpty()) {
                JSONObject siteDetails = getSiteDetails(trimmedId);
                if (siteDetails != null) {
                    searchResults.add(siteDetails);
                }
            }
        }

        notifyDataChanged();
    }

    public void searchMultipleLocationIds(List<String> locationIds) {
        searchResults.clear();

        if (tokenService.getToken().isEmpty()) {
            notifyDataChanged();
            return;
        }

        for (String locationId : locationIds) {
            String trimmedId = locationId.trim();
            if (!trimmedId.isEmpty()) {
                try {
                    String url = "https://chaynssvc.tobit.com/v0.5/" + trimmedId + "/LocationSettings";
                    JSONObject siteDetails = getSiteDetails(url);
                    if (siteDetails != null) {
                        searchResults.add(siteDetails);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        notifyDataChanged();
    }

    public void loadSavedSites() {
        savedSites = new ArrayList<>(chaynsCodesApi.getSavedSites());
        notifyDataChanged();
    }

    private void saveSitesToStorage() {
        chaynsCodesApi.saveSites(savedSites);
    }

    public void addSavedSite(LocationItem site) {
        boolean exists = false;
        for (LocationItem savedSite : savedSites) {
            if (savedSite.getId() == site.getId()) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            savedSites.add(site);
            saveSitesToStorage();
            notifyDataChanged();
        }
    }

    public void removeSavedSite(int locationId) {
        savedSites.removeIf(site -> site.getId() == locationId);
        saveSitesToStorage();
        notifyDataChanged();
    }

    public void clearSearchResults() {
        searchResults.clear();
        notifyDataChanged();
    }

    public List<LocationItem> getSavedSites() {
        return Collections.unmodifiableList(savedSites);
    }

    public List<JSONObject> getSearchResults() {
        return Collections.unmodifiableList(searchResults);
    }

    public boolean isSiteSaved(int locationId) {
        for (LocationItem site : savedSites) {
            if (site.getId() == locationId) {
                return true;
            }
        }
        return false;
    }

    public boolean isTokenEmpty() {
        return tokenService.getToken().isEmpty();
    }

    public String getToken(String siteId) {
        return tokenService.switchSite(siteId);
    }

    public void searchSitesByName(String query) {
        if (tokenService.getToken().isEmpty()) {
            searchResults.clear();
            notifyDataChanged();
            return;
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://relations.chayns.net/relations/location/?query=" + encodedQuery + "&skip=0&take=7";

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + tokenService.getToken());

            ApiResponse response = apiHelper.getRequest(url, headers);

            if (response.isSuccess()) {
                JSONObject jsonResponse = new JSONObject(response.data());
                JSONArray list = jsonResponse.getJSONArray("list");
                searchResults.clear();

                // Process each site to get complete details
                for (int i = 0; i < list.length(); i++) {
                    JSONObject site = list.getJSONObject(i);
                    String siteId = site.optString("siteId", "");

                    if (!siteId.isEmpty()) {
                        // Get detailed site info
                        JSONObject detailedSite = getSiteDetails(siteId);
                        if (detailedSite != null) {
                            searchResults.add(detailedSite);
                        } else {
                            searchResults.add(site);
                        }
                    } else {
                        searchResults.add(site);
                    }
                }

                notifyDataChanged();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            searchResults.clear();
            notifyDataChanged();
            throw new RuntimeException(ex);
        }
    }

    public void searchSiteById(String id) {
        try {
            boolean isSiteId = id.matches("^\\d{5}-\\d{5}$");
            String url;

            if (isSiteId) {
                url = "https://chaynssvc.tobit.com/redirect/v0.5/" + id + "/locationSettings";
            } else {
                url = "https://chaynssvc.tobit.com/v0.5/" + id + "/LocationSettings";
            }

            JSONObject siteDetails = getSiteDetails(url);
            searchResults.clear();

            if (siteDetails != null) {
                searchResults.add(siteDetails);
            }

            notifyDataChanged();
        } catch (Exception ex) {
            ex.printStackTrace();
            searchResults.clear();
            notifyDataChanged();
            throw new RuntimeException(ex);
        }
    }

    private JSONObject getSiteDetails(String urlOrId) {
        String url = urlOrId.startsWith("http") ? urlOrId :
                "https://chaynssvc.tobit.com/redirect/v0.5/" + urlOrId + "/locationSettings";

        try {
            ApiResponse response = apiHelper.getRequest(url);

            if (response.isSuccess()) {
                JSONObject jsonResponse = new JSONObject(response.data());
                return jsonResponse.optJSONObject("data");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}