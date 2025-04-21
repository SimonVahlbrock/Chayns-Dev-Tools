package com.tobit.plugin.controller;

import com.intellij.openapi.project.Project;
import com.tobit.plugin.models.SitesModel;
import com.tobit.plugin.models.data.LocationItem;
import com.tobit.plugin.services.TokenService;
import com.tobit.plugin.views.SitesPanel;
import org.json.JSONObject;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SitesController {
    private final SitesModel model;
    private SitesPanel view;
    private boolean filterDuplicatesEnabled = false;

    public SitesController(Project project) {
        this.model = new SitesModel(project);
        TokenService tokenService = TokenService.getInstance(project);

        // Register as listener for model data changes
        model.addDataChangeListener((savedSites, searchResults) -> {
            if (view != null) {
                List<JSONObject> results = searchResults;

                // Apply duplicate filtering if enabled
                if (filterDuplicatesEnabled && !results.isEmpty()) {
                    // Filter out duplicates based on siteId
                    Map<String, JSONObject> uniqueSites = new HashMap<>();
                    for (JSONObject site : results) {
                        String siteId = site.optString("siteId", "");
                        if (!siteId.isEmpty() && !uniqueSites.containsKey(siteId)) {
                            uniqueSites.put(siteId, site);
                        }
                    }
                    results = new ArrayList<>(uniqueSites.values());
                }

                view.updateData(savedSites, results);

                // Show warning if search yielded no results but was attempted
                if (results.isEmpty() && !model.getSearchResults().isEmpty()) {
                    view.showWarning("No results found");
                }
            }
        });

        // Add token change listener to reload sites when user logs in
        tokenService.addTokenChangeListener(this::handleTokenChange);
    }

    /**
     * Set whether to filter out duplicate site entries from search results
     * @param filterEnabled true to filter duplicates, false to show all results
     */
    public void setFilterDuplicates(boolean filterEnabled) {
        this.filterDuplicatesEnabled = filterEnabled;

        // If we have search results, refresh the view with filtered/unfiltered results
        List<JSONObject> currentResults = model.getSearchResults();
        if (view != null && !currentResults.isEmpty()) {
            if (filterDuplicatesEnabled) {
                // Filter out duplicates based on siteId
                Map<String, JSONObject> uniqueSites = new HashMap<>();
                for (JSONObject site : currentResults) {
                    String siteId = site.optString("siteId", "");
                    if (!siteId.isEmpty() && !uniqueSites.containsKey(siteId)) {
                        uniqueSites.put(siteId, site);
                    }
                }
                view.updateData(model.getSavedSites(), new ArrayList<>(uniqueSites.values()));
            } else {
                view.updateData(model.getSavedSites(), currentResults);
            }
        }
    }

    private void handleTokenChange(String newToken) {
        // When the user logs in (token becomes available), reload the sites
        if (newToken != null && !newToken.isEmpty()) {
            model.loadSavedSites();
        }
    }

    public SitesPanel createView() {
        view = new SitesPanel(this);
        // Update view with current data right after creation
        view.updateData(model.getSavedSites(), model.getSearchResults());
        return view;
    }

    public void performSearch(String searchText) {
        if (searchText.isEmpty()) {
            view.showWarning("Please enter search text");
            return;
        }

        if (model.isTokenEmpty()) {
            view.showWarning("Please login first");
            return;
        }

        try {
            // Pattern for site ID format: #####-#####
            Pattern siteIdPattern = Pattern.compile("\\d{5}-\\d{5}");
            java.util.regex.Matcher siteIdMatcher = siteIdPattern.matcher(searchText);
            List<String> siteIds = new ArrayList<>();

            // Find all site IDs in the search text
            while (siteIdMatcher.find()) {
                siteIds.add(siteIdMatcher.group());
            }

            // If we found site IDs, search for all of them
            if (!siteIds.isEmpty()) {
                model.searchMultipleSiteIds(siteIds);
                return;
            }

            // Pattern for location ID format: number up to 7 digits
            Pattern locationIdPattern = Pattern.compile("\\b\\d{1,7}\\b");
            java.util.regex.Matcher locationIdMatcher = locationIdPattern.matcher(searchText);
            List<String> locationIds = new ArrayList<>();

            // Find all location IDs in the search text
            while (locationIdMatcher.find()) {
                locationIds.add(locationIdMatcher.group());
            }

            // If we found location IDs, search for all of them
            if (!locationIds.isEmpty()) {
                model.searchMultipleLocationIds(locationIds);
                return;
            }

            // If no IDs were found, search by name
            model.searchSitesByName(searchText);
        } catch (Exception ex) {
            ex.printStackTrace();
            view.showError("An error occurred: " + ex.getMessage());
        }
    }

    public void clearSearch() {
        model.clearSearchResults();
    }

    public void reloadSites() {
        if (model.isTokenEmpty()) {
            view.showWarning("Please login first");
            return;
        }

        try {
            model.loadSavedSites();
        } catch (Exception ex) {
            ex.printStackTrace();
            view.showError("Failed to reload: " + ex.getMessage());
        }
    }

    public void saveSite(JSONObject site) {
        String locationName = site.optString("locationName", site.optString("name", ""));
        int locationId = site.optInt("locationId", 0);
        String siteId = site.optString("siteId", "");
        String locationPersonId = site.optString("locationPersonId", "");

        if (locationId > 0) {
            model.addSavedSite(new LocationItem(locationName, locationId, siteId, locationPersonId));
        }
    }

    public void removeSite(int locationId) {
        model.removeSavedSite(locationId);
    }

    public boolean isSiteSaved(int locationId) {
        return model.isSiteSaved(locationId);
    }

    public void getTokenForSite(String siteId) {
        String token = model.getToken(siteId);
        if (token != null) {
            copyToClipboard(token);
        } else {
            view.showError("Failed to get token for site ID: " + siteId);
        }
    }

    public void openSiteInBrowser(String siteId) {
        try {
            String url = "https://chayns.site/" + siteId;
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            ex.printStackTrace();
            view.showError("Failed to open browser: " + ex.getMessage());
        }
    }

    public void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    public List<String> getColumnValues(int columnIndex) {
        List<String> columnValues = new ArrayList<>();
        List<JSONObject> searchResults = model.getSearchResults();

        for (JSONObject site : searchResults) {
            switch (columnIndex) {
                case 0 -> columnValues.add(site.optString("locationName", site.optString("name", "")));
                case 1 -> columnValues.add(site.optString("siteId", ""));
                case 2 -> columnValues.add(Integer.toString(site.optInt("locationId", 0)));
                case 3 -> columnValues.add(site.optString("locationPersonId", ""));
            }
        }

        return columnValues;
    }

    public List<JSONObject> getSearchResults() {
        List<JSONObject> results = model.getSearchResults();

        if (filterDuplicatesEnabled && !results.isEmpty()) {
            // Filter out duplicates based on siteId
            Map<String, JSONObject> uniqueSites = new HashMap<>();
            for (JSONObject site : results) {
                String siteId = site.optString("siteId", "");
                if (!siteId.isEmpty() && !uniqueSites.containsKey(siteId)) {
                    uniqueSites.put(siteId, site);
                }
            }
            return new ArrayList<>(uniqueSites.values());
        }

        return results;
    }
}