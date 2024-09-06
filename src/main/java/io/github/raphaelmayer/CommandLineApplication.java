package io.github.raphaelmayer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.github.raphaelmayer.services.CompositeBaaSOrchestrator;
import io.github.raphaelmayer.services.DeploymentService;
import io.github.raphaelmayer.util.AppConfig;
import io.github.raphaelmayer.util.Utils;

public class CommandLineApplication {

    private final String[] args;
    private final Options options;
    private final CommandLineParser parser;

    public CommandLineApplication(String[] args) {
        this.args = args;
        this.options = buildOptions();
        this.parser = new DefaultParser();
    }

    public void run() {
        try {
            CommandLine cmd = parser.parse(options, args);
            AppConfig appConfig = new AppConfig();
    
            appConfig.setInputFile(cmd.getOptionValue("f"));
            appConfig.setNoDeploy(cmd.hasOption("nodeploy"));
            appConfig.setDebug(cmd.hasOption("debug"));

            if (cmd.hasOption("h")) {
                displayHelp();
            } else if (cmd.hasOption("reset")) {
                resetEnvironment();
            } else if (cmd.hasOption("zip")) {
                Utils.zipFunctions();
            } else if (cmd.hasOption("f")) {
                executeOrchestrator(appConfig);
            } else {
                throw new IllegalArgumentException(
                        "The 'f' option is required unless using the 'reset' or 'zip' option.");
            }

        } catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            displayHelp();
        }
    }

    private void resetEnvironment() {
        System.out.println("Resetting environment...");
        new DeploymentService(null).resetEnvironment();
    }

    private void executeOrchestrator(AppConfig appConfig) {
        new CompositeBaaSOrchestrator(appConfig).run();
    }

    private Options buildOptions() {
        Options options = new Options();

        options.addOption("h", "help", false, "Display usage information and available commands.");
        options.addOption("f", "file", true, "Specify the path to the input file required for workflow generation.");
        options.addOption("reset", false, "Reset the cloud environment without running the workflow generation.");
        options.addOption("zip", false, "Zip all JavaScript (.js) files in the functions directory.");
        options.addOption("nodeploy", false, "Run the workflow generation without deploying resources.");
        options.addOption("debug", false, "Run the workflow generation in debug mode with additional logging.");

        return options;
    }

    private void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        String header = "CompositeBaaS Command-Line Tool\n" +
                "Transform input to output using AWS services and manage workflows.\n\n" +
                "Available Actions:\n" +
                "  -f <arg>          Specify input file to generate a service path and workflow.\n" +
                "  --zip             Zip all functions located in the functions directory.\n" +
                "  --reset           Reset the cloud environment to the initial state.\n" +
                "  -h, --help        Display this help message.\n\n" +
                "Flags (for use with -f):\n" +
                "  --nodeploy        Run without deploying resources.\n" +
                "  --debug           Enable debug mode for detailed output.\n";

        String usage = "Usage:\n" +
                "  CompositeBaaS -f <arg> [--nodeploy] [--debug]\n" +
                "  CompositeBaaS --zip\n" +
                "  CompositeBaaS --reset\n" +
                "  CompositeBaaS -h | --help";

        String footer = "\nExamples:\n" +
                "  java -jar compositebaas.jar -f config.json         Generate workflow using the specified config file\n"
                +
                "  java -jar compositebaas.jar -f config.json --nodeploy --debug\n" +
                "      Generate workflow in debug mode without deployment\n" +
                "  java -jar compositebaas.jar --zip                  Zip all functions in the functions directory\n" +
                "  java -jar compositebaas.jar --reset                Reset the cloud environment\n";

        System.out.println(usage + "\n");
        formatter.printHelp("CompositeBaaS", header, options, footer, false);
    }

}
