package io.github.raphaelmayer.services;

import java.util.List;

import io.github.raphaelmayer.models.Ontology;
import io.github.raphaelmayer.providers.aws.AwsManager;

// Here would be the place to handle different providers. We would need the
// ontology and check which providers we actually need.

public class DeploymentService {

    private final AwsManager awsManager;
    private final Ontology ontology;

    public DeploymentService(Ontology ontology) {
        this.ontology = ontology;
        this.awsManager = new AwsManager();
    }

    public List<String> setupAndDeploy(List<String> servicePath) {
        awsManager.resetEnvironment();
        List<String> functionUrls = awsManager.setupEnvironment(servicePath);
        return functionUrls;
    }

    public void resetEnvironment() {
        awsManager.resetEnvironment();
    }

}
