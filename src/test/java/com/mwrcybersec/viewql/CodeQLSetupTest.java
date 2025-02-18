package com.mwrcybersec.viewql.test;

import com.mwrcybersec.viewql.wrapper.CodeQLEntryPoint;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CodeQLSetupTest {

    @Test
    public void testCodeQLSetup() {
        String cliPath = "/opt/codeql/codeql";
        String extractorsPath = "/opt/codeql/java/tools";
        
        assertDoesNotThrow(() -> {
            CodeQLEntryPoint.setupCodeQLEnvironment(cliPath, extractorsPath, "java");
            
            assertNotNull(System.getProperty("codeql.cli.path"));
        });
    }
}
