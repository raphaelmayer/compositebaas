package io.github.raphaelmayer.util;

public class AppConfig {

    private String inputFile;
    private boolean noDeploy;
    private boolean debug;

    // Getters and setters for each field
    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public boolean isNoDeploy() {
        return noDeploy;
    }

    public void setNoDeploy(boolean noDeploy) {
        this.noDeploy = noDeploy;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}
