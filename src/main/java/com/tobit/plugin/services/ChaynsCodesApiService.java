package com.tobit.plugin.services;

import com.intellij.openapi.project.Project;
import com.tobit.plugin.models.data.ApiResponse;
import com.tobit.plugin.models.data.LocationItem;
import com.tobit.plugin.models.data.Person;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChaynsCodesApiService {
    private static final String API_END_POINT = "https://run.chayns.codes/d017a810";

    private final ApiService apiService;
    private final TokenService tokenService;

    public ChaynsCodesApiService(Project project) {
        this.apiService = new ApiService();
        this.tokenService = TokenService.getInstance(project);
    }

    // Persons methods
    public List<Person> getSavedPersons() {
        String token = tokenService.getTobitDevToken();
        if (token.isEmpty()) return Collections.emptyList();

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("method", "GET_PERSONS");

            ApiResponse response = apiService.postRequest(
                    API_END_POINT,
                    requestBody.toString(),
                    Collections.singletonMap("Authorization", "Bearer " + token)
            );

            if (response.isSuccess()) {
                JSONObject jsonResponse = new JSONObject(response.data());
                JSONArray personsArray = jsonResponse.optJSONArray("persons");

                if (personsArray == null) return Collections.emptyList();

                List<Person> persons = new ArrayList<>();
                for (int i = 0; i < personsArray.length(); i++) {
                    JSONObject personObj = personsArray.optJSONObject(i);
                    if (personObj == null) continue;

                    String userName = personObj.optString("userName", "");
                    String personId = personObj.optString("personId", "");
                    int userId = personObj.optInt("userId", 0);

                    if (!userName.isEmpty() && !personId.isEmpty() && userId > 0) {
                        persons.add(new Person(userName, personId, userId));
                    }
                }
                return persons;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public void savePersons(List<Person> persons) {
        String token = tokenService.getTobitDevToken();
        if (token.isEmpty()) return;

        try {
            JSONArray personsArray = new JSONArray();
            for (Person person : persons) {
                JSONObject personObj = new JSONObject();
                personObj.put("userName", person.userName());
                personObj.put("personId", person.personId());
                personObj.put("userId", person.userId());
                personsArray.put(personObj);
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("method", "SET_PERSONS");
            requestBody.put("persons", personsArray);

            apiService.postRequest(
                    API_END_POINT,
                    requestBody.toString(),
                    Collections.singletonMap("Authorization", "Bearer " + token)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Sites methods
    public List<LocationItem> getSavedSites() {
        String token = tokenService.getTobitDevToken();
        if (token.isEmpty()) return getDefaultSite();

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("method", "GET_SITES");

            ApiResponse response = apiService.postRequest(
                    API_END_POINT,
                    requestBody.toString(),
                    Collections.singletonMap("Authorization", "Bearer " + token)
            );

            if (response.isSuccess()) {
                JSONObject jsonResponse = new JSONObject(response.data());
                JSONArray sitesArray = jsonResponse.optJSONArray("sites");

                if (sitesArray == null) return getDefaultSite();

                List<LocationItem> sites = new ArrayList<>();
                for (int i = 0; i < sitesArray.length(); i++) {
                    JSONObject siteObj = sitesArray.optJSONObject(i);
                    if (siteObj == null) continue;

                    String name = siteObj.optString("name", "");
                    int locationId = siteObj.optInt("locationId", 0);
                    String siteId = siteObj.optString("siteId", "");
                    String locationPersonId = siteObj.optString("locationPersonId", "");

                    if (!name.isEmpty() && locationId > 0 && !siteId.isEmpty()) {
                        sites.add(new LocationItem(name, locationId, siteId, locationPersonId));
                    }
                }

                // Return default site if no sites were found
                if (sites.isEmpty()) {
                    return getDefaultSite();
                }
                return sites;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getDefaultSite();
    }

    public void saveSites(List<LocationItem> sites) {
        String token = tokenService.getTobitDevToken();
        if (token.isEmpty()) return;

        try {
            JSONArray sitesArray = new JSONArray();
            for (LocationItem site : sites) {
                JSONObject siteObj = new JSONObject();
                siteObj.put("name", site.getName());
                siteObj.put("locationId", site.getId());
                siteObj.put("siteId", site.getSiteId());
                siteObj.put("locationPersonId", site.getLocationPersonId());
                sitesArray.put(siteObj);
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("method", "SET_SITES");
            requestBody.put("sites", sitesArray);

            apiService.postRequest(
                    API_END_POINT,
                    requestBody.toString(),
                    Collections.singletonMap("Authorization", "Bearer " + token)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<LocationItem> getDefaultSite() {
        LocationItem defaultSite = new LocationItem("chayns®", 378, "60021-08989", "144-78978");
        saveSites(Collections.singletonList(defaultSite));
        return Collections.singletonList(defaultSite);
    }
}