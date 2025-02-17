package com.mwrcybersec.viewql.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SarifResult {
    private String ruleId;
    private String message;
    private String severity;
    private List<Location> locations;

    public static class Location {
        private String filePath;
        private int startLine;
        private int endLine;
        private String snippet;

        // Getters and setters
    }

    // Getters and setters
}