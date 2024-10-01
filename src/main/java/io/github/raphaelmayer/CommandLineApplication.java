package io.github.raphaelmayer;

import org.apache.commons.cli.*;

import io.github.raphaelmayer.models.AppConfig;
import io.github.raphaelmayer.services.CompositeBaaSOrchestrator;
import io.github.raphaelmayer.services.DeploymentService;
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
        CommandLine cmd = parseCommandLine();
        if (cmd == null) {
            return;
        }

        if (cmd.hasOption("h")) {
            displayHelp();
            return;
        }

        handleCommand(cmd);
    }

    private CommandLine parseCommandLine() {
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            displayHelp();
            return null;
        }
    }

    private void handleCommand(CommandLine cmd) {
        AppConfig appConfig = new AppConfig(cmd);

        if (cmd.hasOption("reset")) {
            resetEnvironment();
        } else if (cmd.hasOption("zip")) {
            zipFunctions();
        } else if (cmd.hasOption("f") && cmd.hasOption("n")) {
            executeOrchestrator(appConfig);
        } else {
            System.err.println("Error: The 'f' and 'n' options are required unless using 'reset' or 'zip'.");
            displayHelp();
        }
    }

    private void resetEnvironment() {
        System.out.println("Resetting environment...");
        new DeploymentService(null).resetEnvironment();
    }

    private void zipFunctions() {
        Utils.zipFunctions();
    }

    private void executeOrchestrator(AppConfig appConfig) {
        new CompositeBaaSOrchestrator(appConfig).run();
    }

    private Options buildOptions() {
        return new Options()
                .addOption("h", "help", false, "Display usage information and available commands.")
                .addOption("f", "file", true, "Specify the path to the input file required for workflow generation.")
                .addOption("n", "name", true, "Specify the name of the workflow.")
                .addOption("reset", false, "Reset the cloud environment without running the workflow generation.")
                .addOption("zip", false, "Zip all JavaScript (.js) files in the functions directory.")
                .addOption("deploy", false, "Run the workflow generation and deploy all required resources.")
                .addOption("debug", false, "Run the workflow generation in debug mode with additional logging.");
    }

    private void displayHelp() {
        String header = "CompositeBaaS Command-Line Tool\n" +
                "Transform input to output using AWS services and manage workflows.\n\n" +
                "Available Actions:\n" +
                "  -f, --file <arg>   Specify input file to generate a service path and workflow (Required).\n" +
                "  -n, --name <arg>   Specify the name of the workflow (Required).\n" +
                "  --zip              Zip all functions located in the functions directory.\n" +
                "  --reset            Reset the cloud environment to the initial state.\n" +
                "  -h, --help         Display this help message.\n\n" +
                "Flags (for use with -f):\n" +
                "  --deploy           Run with deploying all required resources.\n" +
                "  --debug            Enable debug mode for detailed output.\n";

        String usage = "Usage:\n" +
                "  java -jar compositebaas.jar -f <input.json> -n <workflowName> [--deploy] [--debug]\n" +
                "  java -jar compositebaas.jar --zip\n" +
                "  java -jar compositebaas.jar --reset\n" +
                "  java -jar compositebaas.jar -h | --help";

        String footer = "\nExamples:\n" +
                "  java -jar compositebaas.jar -f input.json -n workflowName\n" +
                "      Generate workflow without deploying resources.\n" +
                "  java -jar compositebaas.jar -f input.json -n workflowName --deploy --debug\n" +
                "      Generate workflow in debug mode including deployment.\n" +
                "  java -jar compositebaas.jar --zip\n" +
                "      Zip all functions in the functions directory.\n" +
                "  java -jar compositebaas.jar --reset\n" +
                "      Reset the cloud environment.\n";

        System.out.println(header + "\n" + usage + "\n" + footer);
    }

}
