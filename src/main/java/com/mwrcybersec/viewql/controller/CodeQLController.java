package com.mwrcybersec.viewql.controller;

import com.mwrcybersec.viewql.service.FileService;
import com.mwrcybersec.viewql.wrapper.CodeQLEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.mwrcybersec.viewql.config.CodeQLConfig;
import org.springframework.ui.Model;
import java.util.*;
import com.mwrcybersec.viewql.types.*;

import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/codeql")
public class CodeQLController {

    @Autowired
    private FileService fileService;

    @Autowired
    private CodeQLConfig codeQLConfig;


    @PostMapping("/run-diagnostics")
    public ResponseEntity<Map<String, Object>> runDiagnostics() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Get paths from config
            String cliPath = codeQLConfig.getCli().getPath();
            String extractorsPath = codeQLConfig.getExtractors().getPath();
            
            // Run diagnostics and get detailed output
            DiagnosticResult result = CodeQLEntryPoint.runDiagnostics(cliPath, extractorsPath);
            
            response.put("version", result.getVersionOutput());
            response.put("languages", result.getLanguagesOutput());
            response.put("environment", result.getEnvironmentOutput());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/create-database")
    public ResponseEntity<String> createDatabase(
            @RequestParam("sourceCode") MultipartFile sourceCode,
            @RequestParam("language") String language,
            @RequestParam(required = false) String buildCommand,
            @RequestParam(required = false, defaultValue = "-1") int threads,
            @RequestParam(required = false, defaultValue = "-1") int ram) {
        
        Path extractedPath = null;
        Path dbPath = null;
        
        try {
            // Create unique directories for source and database
            String uniqueId = UUID.randomUUID().toString();
            String baseDir = System.getProperty("java.io.tmpdir") + "/viewql/" + uniqueId;
            extractedPath = Path.of(baseDir + "/source");
            dbPath = Path.of(baseDir + "/db");

            CodeQLEntryPoint.setupCodeQLEnvironment(
                codeQLConfig.getCli().getPath(),
                codeQLConfig.getExtractors().getPath(),
                language
            );

            // Extract the zip file
            fileService.extractZip(sourceCode, extractedPath.toString());

            // Create the CodeQL database
            CodeQLEntryPoint codeQL = new CodeQLEntryPoint.Builder()
                .sourceRoot(extractedPath)
                .dbPath(dbPath)
                .language(language)
                .buildCommand(buildCommand)
                .threads(threads)
                .ram(ram)
                .build();

            int result = codeQL.createDatabase();
            
            // Cleanup source files after database creation
            fileService.cleanup(extractedPath);
            
            return ResponseEntity.ok("Database created at: " + dbPath + " with status: " + result);
        } catch (Exception e) {
            // Cleanup on error
            try {
                if (extractedPath != null) fileService.cleanup(extractedPath);
                if (dbPath != null) fileService.cleanup(dbPath);
            } catch (Exception cleanupError) {
                // Log cleanup error
            }
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}