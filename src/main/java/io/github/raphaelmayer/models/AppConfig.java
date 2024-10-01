package io.github.raphaelmayer.models;

import org.apache.commons.cli.CommandLine;

public class AppConfig {

    private String inputFilePath;
    private String workflowName;
    private boolean deploy;
    private boolean debug;

    public AppConfig(CommandLine cmd) {
        this.setInputFilePath(cmd.getOptionValue("f"));
        this.setWorkflowName(cmd.getOptionValue("n"));
        this.setDeploy(cmd.hasOption("deploy"));
        this.setDebug(cmd.hasOption("debug"));
    }

    public String getInputFilePath() {
        return inputFilePath;
    }

    public void setInputFilePath(String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public boolean isDeploy() {
        return deploy;
    }

    public void setDeploy(boolean deploy) {
        this.deploy = deploy;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}
