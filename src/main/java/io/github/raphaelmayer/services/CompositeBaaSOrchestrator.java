package io.github.raphaelmayer.services;

import java.util.List;

import io.github.raphaelmayer.models.AppConfig;
import io.github.raphaelmayer.models.Ontology;
import io.github.raphaelmayer.models.ServiceFunction;
import io.github.raphaelmayer.models.Transformation;
import io.github.raphaelmayer.util.Constants;
import io.github.raphaelmayer.util.JsonUtils;

public class CompositeBaaSOrchestrator {

    private final PathfindingService pfs;
    private final DeploymentService ds;
    private final FcGenerationService fcs;

    private final Ontology ontology;
    private final Transformation transformation;
    private final AppConfig appConfig;

    public CompositeBaaSOrchestrator(final AppConfig appConfig) {
        this.appConfig = appConfig;
        this.ontology = JsonUtils.parseFile(Constants.ONTOLOGY_PATH, Ontology.class);
        System.out.println("Available functions:");
        this.ontology.functions.forEach(f -> System.out.println(f.name));
        this.transformation = JsonUtils.parseFile(appConfig.getInputFilePath(), Transformation.class);
        String region = (String) transformation.input.get("region");
        System.out.println(this.transformation.toString());

        this.pfs = new PathfindingService(this.ontology);
        // no need for a region, if user does not deploy
        this.ds = new DeploymentService(this.ontology, appConfig.isDeploy() ? region : "nodeploy"); 
        this.fcs = new FcGenerationService(this.ontology);
    }

    public void run() {
        String wfName = this.appConfig.getWorkflowName();

        // path finding
        List<ServiceFunction> servicePath = this.pfs.findServicePath(this.transformation);
        System.out.println("final service path: " + servicePath + "\n");

        // function deployment
        List<String> functionUrls;
        if (this.appConfig.isDeploy()) {
            functionUrls = this.ds.setupAndDeploy(servicePath);
            System.out.println("URL's: " + functionUrls + "\n");
            fcs.createTypeMappingsFile(wfName + "-typemappings.json", servicePath, functionUrls);
        }

        // fc generation (including all files required for execution)
        fcs.createApolloInputFile(this.appConfig.getInputFilePath(), wfName + "-input.json");
        fcs.generateFunctionChoreography(wfName, servicePath, this.transformation);
    }

}
