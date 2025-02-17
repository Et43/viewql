package com.mwrcybersec.viewql.wrapper;

import com.semmle.cli2.CodeQL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.*;
import java.util.Objects;
import java.util.UUID;
import java.nio.file.Files;
import com.mwrcybersec.viewql.types.*;
import com.mwrcybersec.viewql.config.DatabaseConfig;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.*;

public class CodeQLEntryPoint {
    private final Path sourceRoot;
    private Path dbPath;  // Changed to non-final since we update it
    private final String language;
    private String buildCommand;
    private int threads = -1;
    private int ram = -1;
    private final DatabaseConfig databaseConfig;  // Added field

    private CodeQLEntryPoint(Builder builder) {
        this.sourceRoot = Objects.requireNonNull(builder.sourceRoot, "Source root cannot be null");
        this.dbPath = Objects.requireNonNull(builder.dbPath, "Database path cannot be null");
        this.language = Objects.requireNonNull(builder.language, "Language cannot be null");
        this.buildCommand = builder.buildCommand;
        this.threads = builder.threads;
        this.ram = builder.ram;
        this.databaseConfig = Objects.requireNonNull(builder.databaseConfig, "Database config cannot be null");
    }

    public static String getCodeQLVersion() {
        CommandOutput output = new CommandOutput();
        output.start();
        try {
            List<String> args = new ArrayList<>();
            args.add("--version");
            
            int result = CodeQL.mainApi(args.toArray(new String[0]));
            return output.getOutput() + "\nExit code: " + result;
        } finally {
            output.stop();
        }
    }

    public static String resolveLanguages() {
        CommandOutput output = new CommandOutput();
        output.start();
        try {
            // Set required environment variables first
            String cliPath = System.getProperty("codeql.cli.path");
            if (cliPath == null || cliPath.isEmpty()) {
                throw new RuntimeException("CodeQL CLI path not set");
            }
    
            Path codeqlDist = Path.of(cliPath).getParent();
            System.setProperty("CODEQL_DIST", codeqlDist.toString());
            
            // Execute command manually using ProcessBuilder for better output capture
            ProcessBuilder pb = new ProcessBuilder(
                codeqlDist.resolve("codeql.exe").toString(),
                "resolve",
                "languages"
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Read the output
            StringBuilder processOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processOutput.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            String result = processOutput.toString().trim();
            
            // Debug output
            System.out.println("Process exit code: " + exitCode);
            System.out.println("Raw output:\n" + result);
            
            if (exitCode != 0) {
                throw new RuntimeException("Failed to resolve languages. Exit code: " + exitCode);
            }
            
            if (result.isEmpty()) {
                throw new RuntimeException("No languages found in CodeQL installation");
            }
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error resolving languages: " + e.getMessage(), e);
        } finally {
            output.stop();
        }
    }

    public static DiagnosticResult runDiagnostics(String cliPath, String extractorsPath) {
        String versionOutput;
        String languagesOutput;
        String environmentOutput;
    
        CommandOutput output = new CommandOutput();
        output.start();
        try {
            System.out.println("Checking paths:");
            System.out.println("CLI Path exists: " + Files.exists(Path.of(cliPath)));
            System.out.println("Extractors Path exists: " + Files.exists(Path.of(extractorsPath)));
            System.out.println("\nEnvironment Variables:");
            System.out.println("CODEQL_DIST: " + System.getProperty("CODEQL_DIST"));
            System.out.println("CODEQL_JAVA_HOME: " + System.getProperty("CODEQL_JAVA_HOME"));
            System.out.println("CODEQL_EXTRACTOR_JAVA_ROOT: " + System.getProperty("CODEQL_EXTRACTOR_JAVA_ROOT"));
            environmentOutput = output.getOutput();
        } finally {
            output.stop();
        }
    
        versionOutput = getCodeQLVersion();
        languagesOutput = resolveLanguages();
    
        return new DiagnosticResult(versionOutput, languagesOutput, environmentOutput);
    }

    public static void setupCodeQLEnvironment(String cliPath, String extractorsPath, String language) {
        try {
            Path realCliPath = Path.of(cliPath).toRealPath();
            Path codeqlDist = realCliPath.getParent();
            
            // Set system properties
            System.setProperty("CODEQL_DIST", codeqlDist.toString());
            System.setProperty("codeql.cli.path", realCliPath.toString().replace('\\', '/'));
            
            // Set platform-specific variables
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                System.setProperty("CODEQL_PLATFORM", "win64");
                System.setProperty("CODEQL_PLATFORM_DLL_EXTENSION", ".dll");
            }
            
            // Set Java home
            String javaHome = System.getProperty("java.home");
            System.setProperty("CODEQL_JAVA_HOME", javaHome);
            
            // For Java specifically
            if (language.equalsIgnoreCase("java")) {
                Path javaRoot = codeqlDist.resolve("java");
                String javaRootStr = javaRoot.toString().replace('\\', '/');
                System.setProperty("CODEQL_EXTRACTOR_JAVA_ROOT", javaRootStr);
                
                // Verify extractor exists
                Path extractorYml = javaRoot.resolve("codeql-extractor.yml");
                if (!Files.exists(extractorYml)) {
                    throw new RuntimeException("CodeQL Java extractor not found at: " + extractorYml);
                }
            }
            
            // Debug output
            System.out.println("=== CodeQL Environment Setup ===");
            System.out.println("CODEQL_DIST: " + System.getProperty("CODEQL_DIST"));
            System.out.println("CODEQL_PLATFORM: " + System.getProperty("CODEQL_PLATFORM"));
            System.out.println("CODEQL_JAVA_HOME: " + System.getProperty("CODEQL_JAVA_HOME"));
            System.out.println("CODEQL_EXTRACTOR_JAVA_ROOT: " + System.getProperty("CODEQL_EXTRACTOR_JAVA_ROOT"));
            System.out.println("============================");
            
        } catch (Exception e) {
            System.err.println("Error setting up CodeQL environment: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to setup CodeQL environment", e);
        }
    }

    public int createDatabase() {
        try {

            if (databaseConfig == null || databaseConfig.getStorageLocation() == null) {
                throw new IllegalStateException("Database storage location not configured");
            }

            // Create unique database ID and directory
            String dbId = UUID.randomUUID().toString();
            Path dbDir = Path.of(databaseConfig.getStorageLocation(), dbId, "db");
            Files.createDirectories(dbDir);
            
            // Store database metadata
            Path metadataFile = dbDir.getParent().resolve("metadata.json");
            String metadata = String.format(
                "{\"id\": \"%s\", \"language\": \"%s\", \"created\": \"%s\"}",
                dbId,
                language,
                java.time.Instant.now()
            );
            Files.writeString(metadataFile, metadata);
            
            // Update dbPath to use the new location
            this.dbPath = dbDir;

            Path codeqlDist = Path.of(System.getProperty("CODEQL_DIST"));
            ProcessBuilder pb = new ProcessBuilder(
                codeqlDist.resolve("codeql.exe").toString(),
                "database",
                "create",
                "--build-mode=none",
                "--language=" + language,
                "--source-root=" + sourceRoot.toAbsolutePath(),
                this.dbPath.toAbsolutePath().toString()
            );
            
            // Set environment variables for the process
            pb.environment().put("CODEQL_DIST", System.getProperty("CODEQL_DIST"));
            pb.environment().put("CODEQL_JAVA_HOME", System.getProperty("CODEQL_JAVA_HOME"));
            pb.environment().put("CODEQL_PLATFORM", System.getProperty("CODEQL_PLATFORM"));
            pb.environment().put("CODEQL_EXTRACTOR_JAVA_ROOT", System.getProperty("CODEQL_EXTRACTOR_JAVA_ROOT"));
            
            // Add optional arguments
            if (threads > 0) {
                pb.command().add("--threads=" + threads);
            }
            if (ram > 0) {
                pb.command().add("--ram=" + ram);
            }
            if (buildCommand != null && !buildCommand.isEmpty()) {
                pb.command().add("--command=" + buildCommand);
            }
            
            // Debug output
            System.out.println("=== CodeQL Database Creation ===");
            System.out.println("Command: " + String.join(" ", pb.command()));
            System.out.println("Environment:");
            pb.environment().forEach((k, v) -> System.out.println(k + "=" + v));
            System.out.println("============================");
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println(line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Database creation failed: " + output.toString());
            }
            
            return exitCode;
        } catch (Exception e) {
            throw new RuntimeException("Error creating database: " + e.getMessage(), e);
        }
    }


    public static int runSecurityScan(Path dbPath) {
        try {
            // Verify database path exists
            if (!Files.exists(dbPath)) {
                throw new RuntimeException("Database path does not exist: " + dbPath);
            }
    
            // Verify CODEQL_DIST is set
            String codeqlDistStr = System.getProperty("CODEQL_DIST");
            if (codeqlDistStr == null || codeqlDistStr.isEmpty()) {
                throw new RuntimeException("CODEQL_DIST environment variable not set");
            }
    
            Path codeqlDist = Path.of(codeqlDistStr);
            Path codeqlExe = codeqlDist.resolve("codeql.exe");
            
            // Verify CodeQL executable exists
            if (!Files.exists(codeqlExe)) {
                throw new RuntimeException("CodeQL executable not found at: " + codeqlExe);
            }
    
            // Verify query suite exists
            Path querySuite = codeqlDist.resolve("java-security-extended.qls");
    
            // Create results file path
            Path resultsFile = dbPath.getParent().resolve("results.sarif");
    
            ProcessBuilder pb = new ProcessBuilder(
                codeqlExe.toString(),
                "database",
                "analyze",
                "--format=sarif-latest",
                "--output=" + resultsFile.toString(),
                dbPath.toString(),
                "codeql/java-queries:codeql-suites/java-security-extended.qls",
                "--download"
            );
            
            // Set environment variables for the process
            Map<String, String> env = pb.environment();
            env.put("CODEQL_DIST", codeqlDistStr);
            env.put("CODEQL_JAVA_HOME", System.getProperty("CODEQL_JAVA_HOME"));
            env.put("CODEQL_PLATFORM", System.getProperty("CODEQL_PLATFORM", "win64"));
            env.put("PATH", codeqlDistStr + File.pathSeparator + env.get("PATH"));
            
            // Debug output
            System.out.println("=== CodeQL Security Scan ===");
            System.out.println("Database Path: " + dbPath);
            System.out.println("Results File: " + resultsFile);
            System.out.println("Command: " + String.join(" ", pb.command()));
            System.out.println("Environment:");
            env.forEach((k, v) -> System.out.println(k + "=" + v));
            System.out.println("============================");
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println(line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorMsg = String.format(
                    "Security scan failed (exit code %d):\nCommand: %s\nOutput: %s",
                    exitCode,
                    String.join(" ", pb.command()),
                    output.toString()
                );
                throw new RuntimeException(errorMsg);
            }
            
            // Verify results file was created
            if (!Files.exists(resultsFile)) {
                throw new RuntimeException("Security scan completed but results file was not created at: " + resultsFile);
            }
            
            return exitCode;
        } catch (Exception e) {
            String msg = String.format("Error running security scan on database %s: %s", dbPath, e.getMessage());
            System.err.println(msg);
            e.printStackTrace();
            throw new RuntimeException(msg, e);
        }
    }

    public static class Builder {
        private Path sourceRoot;
        private Path dbPath;
        private String language;
        private String buildCommand;
        private int threads = -1;
        private int ram = -1;
        private DatabaseConfig databaseConfig;  // Added field

        public Builder sourceRoot(Path sourceRoot) {
            this.sourceRoot = sourceRoot;
            return this;
        }

        public Builder dbPath(Path dbPath) {
            this.dbPath = dbPath;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder buildCommand(String buildCommand) {
            this.buildCommand = buildCommand;
            return this;
        }

        public Builder threads(int threads) {
            this.threads = threads;
            return this;
        }

        public Builder ram(int ram) {
            this.ram = ram;
            return this;
        }

        public Builder databaseConfig(DatabaseConfig databaseConfig) {  // Added method
            this.databaseConfig = databaseConfig;
            return this;
        }

        public CodeQLEntryPoint build() {
            return new CodeQLEntryPoint(this);
        }
    }
}