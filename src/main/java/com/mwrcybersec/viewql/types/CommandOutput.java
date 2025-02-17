package com.mwrcybersec.viewql.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class CommandOutput {
    private final ByteArrayOutputStream outputStream;
    private final PrintStream originalOut;
    private final PrintStream originalErr;

    public CommandOutput() {
        this.outputStream = new ByteArrayOutputStream();
        this.originalOut = System.out;
        this.originalErr = System.err;
    }

    public void start() {
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);
        System.setErr(printStream);
    }

    public void stop() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    public String getOutput() {
        return outputStream.toString();
    }
}