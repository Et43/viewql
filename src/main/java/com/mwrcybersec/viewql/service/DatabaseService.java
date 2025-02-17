package com.mwrcybersec.viewql.service;

import org.springframework.stereotype.Service;
import com.mwrcybersec.viewql.config.DatabaseConfig;
import com.mwrcybersec.viewql.model.*;
import java.nio.file.*;
import java.util.*;
import java.time.Instant;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import com.mwrcybersec.viewql.wrapper.CodeQLEntryPoint;

@Service
public class DatabaseService {
    private final DatabaseConfig databaseConfig;

    public DatabaseService(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(Path.of(databaseConfig.getStorageLocation()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create database storage directory", e);
        }
    }

    public String getScanResults(String dbId) {
        try {
            Path dbPath = Path.of(databaseConfig.getStorageLocation(), dbId);
            Path resultsFile = dbPath.resolve("results.sarif");
            
            if (!Files.exists(resultsFile)) {
                return "No scan results found";
            }

            return Files.readString(resultsFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read scan results: " + e.getMessage(), e);
        }
    }

    public List<DatabaseInfo> listDatabases() {
        try {
            List<DatabaseInfo> databases = new ArrayList<>();
            Path storageDir = Path.of(databaseConfig.getStorageLocation());
            
            Files.list(storageDir)
                .filter(Files::isDirectory)
                .forEach(path -> {
                    try {
                        Path dbPath = path.resolve("db");
                        Path resultsFile = path.resolve("results.sarif");
                        if (Files.exists(dbPath)) {
                            databases.add(new DatabaseInfo(
                                path.getFileName().toString(),
                                Files.getLastModifiedTime(dbPath).toInstant(),
                                path.toString(),
                                Files.exists(resultsFile)  // Add scan status
                            ));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            
            return databases;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list databases", e);
        }
    }

    // Replace the existing runSecurityScan method
public void runSecurityScan(String dbId) {
    Path dbPath = Path.of(databaseConfig.getStorageLocation(), dbId, "db");
    if (!Files.exists(dbPath)) {
        throw new RuntimeException("Database not found: " + dbId);
    }

    System.out.println("Running security scan for database: " + dbId);
    System.out.println("DB Path: " + dbPath);

    try {
        CodeQLEntryPoint.runSecurityScan(dbPath);
    } catch (Exception e) {
        throw new RuntimeException("Failed to run security scan: " + e.getMessage(), e);
    }
}
}
