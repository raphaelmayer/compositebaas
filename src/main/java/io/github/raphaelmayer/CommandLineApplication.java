package io.github.raphaelmayer;

import org.apache.commons.cli.*;

import io.github.raphaelmayer.services.CompositeBaaSOrchestrator;

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
            if (cmd.hasOption("h")) {
                displayHelp();
            } else if (cmd.hasOption("f")) {
                String inputFilePath = cmd.getOptionValue("f");
                new CompositeBaaSOrchestrator(inputFilePath).run();
            } else if (cmd.hasOption("r")) {
                System.out.println("reset");
            }
        } catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            displayHelp();
        }
    }

    private Options buildOptions() {
        Options options = new Options();

        options.addRequiredOption("f", "file", true, "path to input file");
        options.addOption("h", "help", false, "display help");
        options.addOption("r", "reset", false, "reset the cloud environment");

        return options;
    }

    private void displayHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("CompositeBaaS", options);
    }

}
