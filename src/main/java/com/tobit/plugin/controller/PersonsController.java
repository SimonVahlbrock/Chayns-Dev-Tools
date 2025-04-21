package com.tobit.plugin.controller;

import com.intellij.openapi.project.Project;
import com.tobit.plugin.models.PersonModel;
import com.tobit.plugin.models.data.Person;
import com.tobit.plugin.services.TokenService;
import com.tobit.plugin.views.PersonsPanel;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Controller for the Persons panel.
 * Coordinates between PersonModel and PersonsView.
 */
public class PersonsController {
    private final PersonModel model;
    private PersonsPanel view;
    private boolean filterDuplicatesEnabled = false;

    public PersonsController(Project project) {
        this.model = new PersonModel(project);
        TokenService tokenService = TokenService.getInstance(project);

        // Register as listener for model data changes
        this.model.addDataChangeListener(new PersonModel.DataChangeListener() {
            @Override
            public void onDataChanged(List<Person> savedPersons, List<Person> searchResults) {
                if (view != null) {
                    List<Person> results = searchResults;

                    // Apply duplicate filtering if enabled
                    if (filterDuplicatesEnabled && !results.isEmpty()) {
                        // Filter out duplicates based on personId
                        Map<String, Person> uniquePersons = new HashMap<>();
                        for (Person person : results) {
                            if (!uniquePersons.containsKey(person.personId())) {
                                uniquePersons.put(person.personId(), person);
                            }
                        }
                        results = new ArrayList<>(uniquePersons.values());
                    }

                    view.updateData(savedPersons, results);

                    // Show warning if search yielded no results but was attempted
                    if (results.isEmpty() && !model.getSearchResults().isEmpty()) {
                        view.showWarning("No results found");
                    }
                }
            }
        });

        // Add token change listener to reload sites when user logs in
        tokenService.addTokenChangeListener(this::handleTokenChange);
    }

    /**
     * Set whether to filter out duplicate person entries from search results
     * @param filterEnabled true to filter duplicates, false to show all results
     */
    public void setFilterDuplicates(boolean filterEnabled) {
        this.filterDuplicatesEnabled = filterEnabled;

        // Re-apply filtering to current search results if any exist
        List<Person> currentResults = model.getSearchResults();
        if (view != null && !currentResults.isEmpty()) {
            if (filterDuplicatesEnabled) {
                // Filter out duplicates based on personId
                Map<String, Person> uniquePersons = new HashMap<>();
                for (Person person : currentResults) {
                    if (!uniquePersons.containsKey(person.personId())) {
                        uniquePersons.put(person.personId(), person);
                    }
                }
                view.updateData(model.getSavedPersons(), new ArrayList<>(uniquePersons.values()));
            } else {
                view.updateData(model.getSavedPersons(), currentResults);
            }
        }
    }

    private void handleTokenChange(String newToken) {
        // When the user logs in (token becomes available), reload the sites
        if (newToken != null && !newToken.isEmpty()) {
            model.loadSavedPersons();
        }
    }

    /**
     * Creates and returns the view for this controller
     */
    public PersonsPanel createView() {
        view = new PersonsPanel(this);
        // Update view with current data right after creation
        view.updateData(model.getSavedPersons(), model.getSearchResults());
        return view;
    }

    /**
     * Performs a search if conditions are valid
     */
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
            Pattern personIdPattern = Pattern.compile("[0-9a-zA-Z]{3}-[0-9a-zA-Z]{5}");
            java.util.regex.Matcher personIdMatcher = personIdPattern.matcher(searchText);
            List<String> personIds = new ArrayList<>();

            // Find all person IDs in the search text
            while (personIdMatcher.find()) {
                personIds.add(personIdMatcher.group());
            }

            // If we found person IDs, search for all of them
            if (!personIds.isEmpty()) {
                model.searchMultiplePersonIds(personIds);
                return;
            }

            // If no personIds were found, do a regular search
            model.searchPersons(searchText);
        } catch (Exception ex) {
            ex.printStackTrace();
            view.showError("An error occurred: " + ex.getMessage());
        }
    }

    /**
     * Clears search results
     */
    public void clearSearch() {
        model.clearSearchResults();
    }

    /**
     * Reloads persons data from API
     */
    public void reloadPersons() {
        if (model.isTokenEmpty()) {
            view.showWarning("Please login first");
            return;
        }

        try {
            model.loadSavedPersons();
        } catch (Exception ex) {
            ex.printStackTrace();
            view.showError("Failed to reload: " + ex.getMessage());
        }
    }

    /**
     * Saves a person to favorites
     */
    public void savePerson(Person person) {
        model.addSavedPerson(person);
    }

    /**
     * Removes a person from favorites
     */
    public void removePerson(String personId) {
        model.removeSavedPerson(personId);
    }

    /**
     * Checks if a person is saved
     */
    public boolean isPersonSaved(String personId) {
        return model.isPersonSaved(personId);
    }

    /**
     * Copies text to system clipboard
     */
    public void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    /**
     * Adds a custom person
     */
    public void addCustomPerson(String firstName, String lastName, String personId, int userId) {
        Person person = new Person(firstName + " " + lastName, personId, userId);
        model.addSavedPerson(person);
    }

    public List<String> getColumnValues(int columnIndex) {
        List<String> columnValues = new ArrayList<>();
        List<Person> searchResults = model.getSearchResults();

        for (Person person : searchResults) {
            switch (columnIndex) {
                case 0 -> columnValues.add(person.userName());
                case 1 -> columnValues.add(person.personId());
                case 2 -> columnValues.add(Integer.toString(person.userId()));
            }
        }

        return columnValues;
    }

    public List<Person> getSearchResults() {
        List<Person> results = model.getSearchResults();

        if (filterDuplicatesEnabled && !results.isEmpty()) {
            // Filter out duplicates based on personId
            Map<String, Person> uniquePersons = new HashMap<>();
            for (Person person : results) {
                if (!uniquePersons.containsKey(person.personId())) {
                    uniquePersons.put(person.personId(), person);
                }
            }
            return new ArrayList<>(uniquePersons.values());
        }

        return results;
    }
}