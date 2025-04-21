package com.tobit.plugin.models;

import com.intellij.openapi.project.Project;
import com.tobit.plugin.models.data.ApiResponse;
import com.tobit.plugin.models.data.Person;
import com.tobit.plugin.services.ApiService;
import com.tobit.plugin.services.ChaynsCodesApiService;
import com.tobit.plugin.services.TokenService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class PersonModel {
    private final ChaynsCodesApiService chaynsCodesApi;
    private final ApiService apiHelper;
    private final TokenService tokenService;

    // Data stores
    private List<Person> savedPersons = new ArrayList<>();
    private List<Person> searchResults = new ArrayList<>();

    // Listeners for data changes
    private final List<DataChangeListener> dataChangeListeners = new ArrayList<>();

    public interface DataChangeListener {
        void onDataChanged(List<Person> savedPersons, List<Person> searchResults);
    }

    // Initialize data
    public PersonModel(Project project) {
        this.chaynsCodesApi = new ChaynsCodesApiService(project);
        this.apiHelper = new ApiService();
        this.tokenService = TokenService.getInstance(project);

        loadSavedPersons();
    }

    public void addDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.add(listener);
    }

    public void removeDataChangeListener(DataChangeListener listener) {
        dataChangeListeners.remove(listener);
    }

    private void notifyDataChanged() {
        for (DataChangeListener listener : dataChangeListeners) {
            listener.onDataChanged(savedPersons, searchResults);
        }
    }

    public void loadSavedPersons() {
        savedPersons = new ArrayList<>(chaynsCodesApi.getSavedPersons());
        notifyDataChanged();
    }

    private void savePersons() {
        chaynsCodesApi.savePersons(savedPersons);
    }

    public void addSavedPerson(Person person) {
        boolean exists = false;
        for (Person saved : savedPersons) {
            if (saved.personId().equals(person.personId())) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            savedPersons.add(person);
            savePersons();
            notifyDataChanged();
        }
    }

    public void removeSavedPerson(String personId) {
        savedPersons.removeIf(person -> person.personId().equals(personId));
        savePersons();
        notifyDataChanged();
    }

    public void searchPersons(String searchString) {
        if (tokenService.getToken().isEmpty()) {
            searchResults.clear();
            notifyDataChanged();
            return;
        }

        searchResults = new ArrayList<>(getPersonsBySearchString(searchString));

        notifyDataChanged();
    }

    public void clearSearchResults() {
        searchResults.clear();
        notifyDataChanged();
    }

    public List<Person> getSavedPersons() {
        return new ArrayList<>(savedPersons);
    }

    public List<Person> getSearchResults() {
        return new ArrayList<>(searchResults);
    }

    public boolean isPersonSaved(String personId) {
        for (Person person : savedPersons) {
            if (person.personId().equals(personId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTokenEmpty() {
        return tokenService.getToken().isEmpty();
    }

    public void searchMultiplePersonIds(List<String> personIds) {
        searchResults.clear();

        if (tokenService.getToken().isEmpty()) {
            notifyDataChanged();
            return;
        }

        List<Person> foundPersons = new ArrayList<>();

        for (String personId : personIds) {
            String trimmedId = personId.trim();
            if (!trimmedId.isEmpty()) {
                try {
                    // Use the same URL pattern as in getPersonsBySearchString but with personId as search
                    String url = "https://relations.chayns.net/relations/v2/person?searchString=" +
                            URLEncoder.encode(trimmedId, StandardCharsets.UTF_8) +
                            "&take=7&scoreForSite=1";

                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + tokenService.getToken());

                    ApiResponse response = apiHelper.getRequest(url, headers);

                    if (response.isSuccess()) {
                        JSONObject jsonResponse = new JSONObject(response.data());
                        JSONArray list = jsonResponse.getJSONArray("list");

                        // Find exact match for personId in results
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject personJson = list.getJSONObject(i);
                            String resultPersonId = personJson.getString("personId");

                            if (resultPersonId.equals(trimmedId)) {
                                Person person = new Person(
                                        personJson.getString("firstName") + " " + personJson.getString("lastName"),
                                        personJson.getString("personId"),
                                        personJson.getInt("userId")
                                );
                                foundPersons.add(person);
                                break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        searchResults.addAll(foundPersons);
        notifyDataChanged();
    }

    private List<Person> getPersonsBySearchString(String searchString) {
        try {
            String encodedSearch = URLEncoder.encode(searchString, StandardCharsets.UTF_8);
            String url = "https://relations.chayns.net/relations/v2/person?searchString=" + encodedSearch + "&take=7&scoreForSite=1";

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + tokenService.getToken());

            ApiResponse response = apiHelper.getRequest(url, headers);

            if (response.isSuccess()) {
                JSONObject jsonResponse = new JSONObject(response.data());
                JSONArray list = jsonResponse.getJSONArray("list");

                List<Person> persons = new ArrayList<>();
                for (int i = 0; i < list.length(); i++) {
                    JSONObject personJson = list.getJSONObject(i);
                    Person person = new Person(
                            personJson.getString("firstName") + " " + personJson.getString("lastName"),
                            personJson.getString("personId"),
                            personJson.getInt("userId")
                    );
                    persons.add(person);
                }
                return persons;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ArrayList<>();
    }
}