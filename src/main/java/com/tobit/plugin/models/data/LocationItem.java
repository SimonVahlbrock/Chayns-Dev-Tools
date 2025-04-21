package com.tobit.plugin.models.data;

import java.util.Objects;

public class LocationItem {
    private final String name;
    private final int id;
    private final String siteId;
    private final String locationPersonId;

    public LocationItem(String name, int id, String siteId, String locationPersonId) {
        this.name = name;
        this.id = id;
        this.siteId = siteId;
        this.locationPersonId = locationPersonId;
    }

    public LocationItem(String name, int id) {
        this(name, id, "", "");
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getLocationPersonId() {
        return locationPersonId;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationItem that = (LocationItem) o;
        return id == that.id &&
                Objects.equals(name, that.name) &&
                Objects.equals(siteId, that.siteId) &&
                Objects.equals(locationPersonId, that.locationPersonId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, siteId, locationPersonId);
    }
}