package com.mwrcybersec.viewql.wrapper;

import com.semmle.cli2.CodeQL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.nio.file.Files;
import com.mwrcybersec.viewql.types.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

public class CodeQLEntryPoint {
    private final Path sourceRoot;
    private final Path dbPath;
    private final String language;
    private String buildCommand;
    private int threads = -1;
    private int ram = -1;

    private CodeQLEntryPoint(Builder builder) {
        this.sourceRoot = Objects.requireNonNull(builder.sourceRoot, "Source root cannot be null");
        this.dbPath = Objects.requireNonNull(builder.dbPath, "Database path cannot be null");
        this.language = Objects.requireNonNull(builder.language, "Language cannot be null");
        this.buildCommand = builder.buildCommand;
        this.threads = builder.threads;
        this.ram = builder.ram;
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
            Path codeqlDist = Path.of(System.getProperty("CODEQL_DIST"));
            ProcessBuilder pb = new ProcessBuilder(
                codeqlDist.resolve("codeql.exe").toString(),
                "database",
                "create",
                "--build-mode=none",
                "--language=" + language,
                "--source-root=" + sourceRoot.toAbsolutePath(),
                dbPath.toAbsolutePath().toString()
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

    public static class Builder {
        private Path sourceRoot;
        private Path dbPath;
        private String language;
        private String buildCommand;
        private int threads = -1;
        private int ram = -1;

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

        public CodeQLEntryPoint build() {
            return new CodeQLEntryPoint(this);
        }
    }
}