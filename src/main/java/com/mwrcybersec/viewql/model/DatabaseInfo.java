package com.mwrcybersec.viewql.model;

import java.time.Instant;

public class DatabaseInfo {
    private final String id;
    private final Instant created;
    private final String path;
    private final boolean hasResults;  // Added field

    public DatabaseInfo(String id, Instant created, String path, boolean hasResults) {
        this.id = id;
        this.created = created;
        this.path = path;
        this.hasResults = hasResults;
    }

    public String getId() {
        return id;
    }

    public Instant getCreated() {
        return created;
    }

    public String getPath() {
        return path;
    }

    public boolean hasResults() {
        return hasResults;
    }
}