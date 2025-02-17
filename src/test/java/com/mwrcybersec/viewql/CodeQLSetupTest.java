package com.mwrcybersec.viewql.test;

import com.mwrcybersec.viewql.wrapper.CodeQLEntryPoint;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CodeQLSetupTest {

    @Test
    public void testCodeQLSetup() {
        String cliPath = "C:/Users/ethan/Downloads/codeql-win64/codeql/codeql.exe";
        String extractorsPath = "C:/Users/ethan/Downloads/codeql-win64/codeql/java/tools";
        
        assertDoesNotThrow(() -> {
            CodeQLEntryPoint.setupCodeQLEnvironment(cliPath, extractorsPath, "java");
            
            assertNotNull(System.getProperty("codeql.cli.path"));
            assertNotNull(System.getProperty("codeql.extractors.path"));
            assertNotNull(System.getProperty("CODEQL_EXTRACTOR_JAVA_ROOT"));
        });
    }
}