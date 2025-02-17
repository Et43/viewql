package com.mwrcybersec.viewql.types;

public class DiagnosticResult {
    private final String versionOutput;
    private final String languagesOutput;
    private final String environmentOutput;

    public DiagnosticResult(String versionOutput, String languagesOutput, String environmentOutput) {
        this.versionOutput = versionOutput;
        this.languagesOutput = languagesOutput;
        this.environmentOutput = environmentOutput;
    }

    public String getVersionOutput() { return versionOutput; }
    public String getLanguagesOutput() { return languagesOutput; }
    public String getEnvironmentOutput() { return environmentOutput; }
}
