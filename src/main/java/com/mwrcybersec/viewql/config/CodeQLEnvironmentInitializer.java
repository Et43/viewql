package com.mwrcybersec.viewql.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.mwrcybersec.viewql.wrapper.CodeQLEntryPoint;
import java.nio.file.Path;

@Component
public class CodeQLEnvironmentInitializer {
    
    private final CodeQLConfig codeQLConfig;
    
    @Autowired
    public CodeQLEnvironmentInitializer(CodeQLConfig codeQLConfig) {
        this.codeQLConfig = codeQLConfig;
    }
    
    @EventListener(ApplicationStartedEvent.class)
    public void initializeCodeQLEnvironment() {
        try {
            // Set up the basic CodeQL environment
            CodeQLEntryPoint.setupCodeQLEnvironment(
                codeQLConfig.getCli().getPath(),
                codeQLConfig.getExtractors().getPath(),
                "java"  // Default to Java since we're mainly working with it
            );
            
            // Additional verification
            verifyEnvironment();
            
        } catch (Exception e) {
            // Log error but don't prevent application startup
            System.err.println("Failed to initialize CodeQL environment: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void verifyEnvironment() {
        // Verify essential properties are set
        String[] requiredProps = {
            "CODEQL_DIST",
            "CODEQL_JAVA_HOME",
            "CODEQL_PLATFORM",
            "CODEQL_EXTRACTOR_JAVA_ROOT"
        };
        
        for (String prop : requiredProps) {
            String value = System.getProperty(prop);
            if (value == null || value.isEmpty()) {
                throw new RuntimeException("Required property not set: " + prop);
            }
            System.out.println("Verified " + prop + ": " + value);
        }
        
        // Verify paths exist
        Path codeqlDist = Path.of(System.getProperty("CODEQL_DIST"));
        Path codeqlExe = codeqlDist.resolve("codeql");
        Path querySuite = codeqlDist.resolve("java-security-extended.qls");
        
        if (!codeqlExe.toFile().exists()) {
            throw new RuntimeException("CodeQL executable not found at: " + codeqlExe);
        }
        
        if (!querySuite.toFile().exists()) {
            System.out.println("Warning: Query suite not found at: " + querySuite);
            System.out.println("Please ensure java-security-extended.qls is available in the CodeQL directory");
        }
    }
}
