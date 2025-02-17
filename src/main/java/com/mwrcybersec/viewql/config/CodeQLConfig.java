package com.mwrcybersec.viewql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "codeql")
public class CodeQLConfig {
    private Cli cli = new Cli();
    private Extractors extractors = new Extractors();

    public static class Cli {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class Extractors {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public Cli getCli() {
        return cli;
    }

    public Extractors getExtractors() {
        return extractors;
    }
}