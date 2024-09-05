package io.github.raphaelmayer.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.raphaelmayer.models.Ontology;
import io.github.raphaelmayer.models.Transformation;
import io.github.raphaelmayer.util.AppConfig;
import io.github.raphaelmayer.util.Constants;
import io.github.raphaelmayer.util.JsonUtils;

public class CompositeBaaSOrchestrator {

    private final PathfindingService pfs;
    private final DeploymentService ds;
    private final FcGenerationService fcs;

    private final Ontology ontology;
    private final Transformation transformation;
    private final AppConfig appConfig;

    public CompositeBaaSOrchestrator(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.ontology = JsonUtils.parseFile(Constants.ONTOLOGY_PATH, Ontology.class);
        System.out.println("Available functions:");
        this.ontology.functions.forEach(f -> System.out.println(f.name));
        this.transformation = JsonUtils.parseFile(appConfig.getInputFile(), Transformation.class);
        System.out.println(this.transformation.toString());

        this.pfs = new PathfindingService(this.ontology);
        this.ds = new DeploymentService(this.ontology);
        this.fcs = new FcGenerationService(this.ontology);
    }

    public void run() {
        // path finding
        List<String> servicePath = this.pfs.findServicePath(this.transformation);
        System.out.println("final service path: " + servicePath + "\n");

        // function deployment
        List<String> functionUrls;
        if (!this.appConfig.isNoDeploy()) {
            functionUrls = this.ds.setupAndDeploy(servicePath);
        } else {
            functionUrls = servicePath.stream()
                    .map(serviceName -> "https://" + serviceName)
                    .collect(Collectors.toList());
        }
        System.out.println("URL's: " + functionUrls + "\n");

        // fc generation (including all files required for execution)
        fcs.generateFunctionChoreography("someTestName", servicePath, this.transformation);
        fcs.createTypeMappingsFile("type_mappings.json", servicePath, functionUrls);
        fcs.createApolloInputFile(this.appConfig.getInputFile(), "apollo-input.json");
    }

}
