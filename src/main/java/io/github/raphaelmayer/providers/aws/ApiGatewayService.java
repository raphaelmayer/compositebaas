package io.github.raphaelmayer.providers.aws;

import java.util.List;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.ApiGatewayException;
import software.amazon.awssdk.services.apigateway.model.CreateDeploymentRequest;
import software.amazon.awssdk.services.apigateway.model.CreateResourceRequest;
import software.amazon.awssdk.services.apigateway.model.CreateResourceResponse;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.CreateRestApiResponse;
import software.amazon.awssdk.services.apigateway.model.DeleteDeploymentRequest;
import software.amazon.awssdk.services.apigateway.model.DeleteMethodRequest;
import software.amazon.awssdk.services.apigateway.model.DeleteRestApiRequest;
import software.amazon.awssdk.services.apigateway.model.DeleteStageRequest;
import software.amazon.awssdk.services.apigateway.model.Deployment;
import software.amazon.awssdk.services.apigateway.model.GetDeploymentsRequest;
import software.amazon.awssdk.services.apigateway.model.GetDeploymentsResponse;
import software.amazon.awssdk.services.apigateway.model.GetResourceRequest;
import software.amazon.awssdk.services.apigateway.model.GetResourceResponse;
import software.amazon.awssdk.services.apigateway.model.GetResourcesRequest;
import software.amazon.awssdk.services.apigateway.model.GetResourcesResponse;
import software.amazon.awssdk.services.apigateway.model.GetRestApisResponse;
import software.amazon.awssdk.services.apigateway.model.GetStagesRequest;
import software.amazon.awssdk.services.apigateway.model.GetStagesResponse;
import software.amazon.awssdk.services.apigateway.model.IntegrationType;
import software.amazon.awssdk.services.apigateway.model.PutIntegrationRequest;
import software.amazon.awssdk.services.apigateway.model.PutMethodRequest;
import software.amazon.awssdk.services.apigateway.model.Resource;
import software.amazon.awssdk.services.apigateway.model.RestApi;
import software.amazon.awssdk.services.apigateway.model.Stage;

public class ApiGatewayService {

    private final ApiGatewayClient apiGatewayClient;
    private final String region;

    public ApiGatewayService(ApiGatewayClient apiGatewayClient, String region) {
        this.apiGatewayClient = apiGatewayClient;
        this.region = region;
    }

    /**
     * Create an API Gateway.
     * 
     * @param apiName Name of the API Gateway
     * @return The API ID of the newly created API Gateway
     */
    public String createApiGateway(String apiName) {
        try {
            CreateRestApiResponse response = apiGatewayClient.createRestApi(CreateRestApiRequest.builder()
                    .name(apiName)
                    .description("API Gateway to expose multiple Lambda functions")
                    .build());
            return response.id();
        } catch (ApiGatewayException e) {
            System.err.println("Failed to create API Gateway: " + e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    /**
     * Deploy the API Gateway to a specific stage.
     * 
     * @param apiId     API ID of the Gateway
     * @param stageName Stage to deploy to (e.g., prod)
     */
    public void deployApi(String apiId, String stageName) {
        try {
            apiGatewayClient.createDeployment(CreateDeploymentRequest.builder()
                    .restApiId(apiId)
                    .stageName(stageName)
                    .build());
        } catch (ApiGatewayException e) {
            System.err.println("Failed to deploy API Gateway: " + e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    public String getRootResourceId(String apiId) {
        GetResourcesResponse resourcesResponse = apiGatewayClient
                .getResources(GetResourcesRequest.builder().restApiId(apiId).build());
        return resourcesResponse.items().stream().filter(resource -> resource.path().equals("/")).findFirst()
                .orElseThrow().id();
    }

    public String createApiResource(String apiId, String parentId, String pathPart) {
        CreateResourceResponse resourceResponse = apiGatewayClient.createResource(CreateResourceRequest.builder()
                .restApiId(apiId)
                .parentId(parentId)
                .pathPart(pathPart)
                .build());
        return resourceResponse.id();
    }

    public void createApiMethod(String apiId, String resourceId, String httpMethod) {
        apiGatewayClient.putMethod(PutMethodRequest.builder()
                .restApiId(apiId)
                .resourceId(resourceId)
                .httpMethod(httpMethod)
                .authorizationType("NONE") // Public access
                .build());
    }

    public void integrateApiWithLambda(String apiId, String resourceId, String httpMethod, String lambdaArn) {
        apiGatewayClient.putIntegration(PutIntegrationRequest.builder()
                .restApiId(apiId)
                .resourceId(resourceId)
                .httpMethod(httpMethod)
                .type(IntegrationType.AWS_PROXY)
                .integrationHttpMethod("POST")
                .uri("arn:aws:apigateway:" + this.region + ":lambda:path/2015-03-31/functions/" + lambdaArn
                        + "/invocations")
                .build());
    }

    /**
     * Deletes the specified API Gateway and all associated resources.
     * 
     * @param apiId The API ID of the Gateway to delete
     */
    public void deleteApiGateway(String apiId) {
        try {
            // Get all the resources associated with the API
            List<Resource> resources = getAllApiResources(apiId);

            // For each resource, delete the methods
            for (Resource resource : resources) {
                deleteMethodsForResource(apiId, resource.id());
            }

            // Delete all stages associated with the API
            deleteApiStages(apiId);

            // Delete deployments
            deleteApiDeployments(apiId);

            // Finally, delete the API Gateway itself
            apiGatewayClient.deleteRestApi(DeleteRestApiRequest.builder()
                    .restApiId(apiId)
                    .build());
        } catch (ApiGatewayException e) {
            System.err.println("Failed to delete API Gateway: " + e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    /**
     * Get all resources of the specified API Gateway.
     * 
     * @param apiId The API ID of the Gateway
     * @return List of resources
     */
    private List<Resource> getAllApiResources(String apiId) {
        GetResourcesResponse resourcesResponse = apiGatewayClient.getResources(
                GetResourcesRequest.builder().restApiId(apiId).build());

        return resourcesResponse.items();
    }

    /**
     * Deletes all methods (GET, POST, etc.) associated with a resource.
     * 
     * @param apiId      The API ID of the Gateway
     * @param resourceId The ID of the resource to clean up
     */
    private void deleteMethodsForResource(String apiId, String resourceId) {
        GetResourceResponse resource = apiGatewayClient.getResource(
                GetResourceRequest.builder().restApiId(apiId).resourceId(resourceId).build());

        // Iterate through all HTTP methods (GET, POST, etc.) and delete them
        for (String httpMethod : resource.resourceMethods().keySet()) {
            apiGatewayClient.deleteMethod(DeleteMethodRequest.builder()
                    .restApiId(apiId)
                    .resourceId(resourceId)
                    .httpMethod(httpMethod)
                    .build());
        }
    }

    /**
     * Delete all stages associated with the API Gateway.
     * 
     * @param apiId The API ID of the Gateway
     */
    private void deleteApiStages(String apiId) {
        try {
            GetStagesResponse stagesResponse = apiGatewayClient.getStages(GetStagesRequest.builder()
                    .restApiId(apiId)
                    .build());

            for (Stage stage : stagesResponse.item()) {
                apiGatewayClient.deleteStage(DeleteStageRequest.builder()
                        .restApiId(apiId)
                        .stageName(stage.stageName())
                        .build());
            }

        } catch (ApiGatewayException e) {
            System.err.println("Failed to delete stages for API Gateway: " + e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    /**
     * Deletes all deployments of the specified API Gateway.
     * 
     * @param apiId The API ID of the Gateway
     */
    private void deleteApiDeployments(String apiId) {
        GetDeploymentsResponse deploymentsResponse = apiGatewayClient.getDeployments(
                GetDeploymentsRequest.builder().restApiId(apiId).build());

        // Iterate through all deployments and delete them
        for (Deployment deployment : deploymentsResponse.items()) {
            apiGatewayClient.deleteDeployment(DeleteDeploymentRequest.builder()
                    .restApiId(apiId)
                    .deploymentId(deployment.id())
                    .build());
        }
    }

    public List<String> listApiGatewaysByName(String apiName) {
        GetRestApisResponse response = apiGatewayClient.getRestApis();
        return response.items().stream()
                .filter(api -> api.name().equals(apiName))
                .map(RestApi::id)
                .collect(Collectors.toList());
    }

}
