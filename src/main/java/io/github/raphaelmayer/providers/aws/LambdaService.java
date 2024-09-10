package io.github.raphaelmayer.providers.aws;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.github.raphaelmayer.models.ServiceFunction;
import io.github.raphaelmayer.util.Constants;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest;
import software.amazon.awssdk.services.lambda.model.DeleteLayerVersionRequest;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.LambdaException;
import software.amazon.awssdk.services.lambda.model.LayerVersionContentInput;
import software.amazon.awssdk.services.lambda.model.LayerVersionsListItem;
import software.amazon.awssdk.services.lambda.model.LayersListItem;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;
import software.amazon.awssdk.services.lambda.model.ListLayerVersionsRequest;
import software.amazon.awssdk.services.lambda.model.ListLayerVersionsResponse;
import software.amazon.awssdk.services.lambda.model.ListLayersResponse;
import software.amazon.awssdk.services.lambda.model.PublishLayerVersionRequest;
import software.amazon.awssdk.services.lambda.model.PublishLayerVersionResponse;

public class LambdaService {

    private final LambdaClient lambdaClient;
    private final String region;
    private final String accountId;

    public LambdaService(LambdaClient lambdaClient, String region, String accountId) {
        this.lambdaClient = lambdaClient;
        this.region = region;
        this.accountId = accountId;
    }

    public String uploadLambda(ServiceFunction function, String zipFilePath, String roleArn, List<String> layerArns) {
        Path path = Paths.get(zipFilePath);

        String runtime = (function.config.runtime != null) ? function.config.runtime : Constants.LAMBDA_RUNTIME;
        Integer memorySize = (function.config.memory != null) ? function.config.memory : 128; // Default 128 MB
        Integer timeout = (function.config.timeout != null) ? function.config.timeout : 3; // Default 3 seconds

        try {
            SdkBytes lambdaFunctionCode = SdkBytes.fromByteArray(Files.readAllBytes(path));
            CreateFunctionRequest request = CreateFunctionRequest.builder()
                    .functionName(Constants.LAMBDA_FUNCTION_PREFIX + function.name)
                    .role(roleArn)
                    .layers(layerArns)
                    .handler(function.name + ".handler")
                    .runtime(runtime)
                    .memorySize(memorySize)
                    .timeout(timeout)
                    .code(software.amazon.awssdk.services.lambda.model.FunctionCode.builder()
                            .zipFile(lambdaFunctionCode)
                            .build())
                    .build();
            CreateFunctionResponse response = lambdaClient.createFunction(request);
            return response.functionArn();

        } catch (IOException e) {
            e.printStackTrace(); // Consider proper error handling or logging here
            throw new RuntimeException("Error creating Lambda function: " + e.getMessage(), e);
        }
    }

    public String uploadLambda(ServiceFunction function, String zipFilePath, String roleArn) {
        return uploadLambda(function, zipFilePath, roleArn, Collections.emptyList());
    }

    public void deleteLambda(String functionName) {
        DeleteFunctionRequest request = DeleteFunctionRequest.builder()
                .functionName(functionName)
                .build();
        lambdaClient.deleteFunction(request);
    }

    public void addLambdaInvokePermission(String lambdaArn, String apiId, String resourcePath, String httpMethod) {
        lambdaClient.addPermission(builder -> builder
                .functionName(lambdaArn)
                .statementId(resourcePath + "-Invoke")
                .action("lambda:InvokeFunction")
                .principal("apigateway.amazonaws.com")
                .sourceArn(
                        "arn:aws:execute-api:" + this.region + ":" + accountId + ":" + apiId + "/prod/"
                                + httpMethod + "/"
                                + resourcePath));
    }

    /**
     * Creates a Lambda Layer with the given libraries, which can then be used by
     * Lambda functions.
     *
     * @param layerName   The name of the Lambda Layer.
     * @param zipFilePath The path to the ZIP file containing the libraries.
     * @param runtime     The runtime environment for the layer (e.g., "nodejs14.x",
     *                    "python3.8").
     * @return The ARN of the created Lambda Layer version.
     */
    public String createLayer(String layerName, String zipFilePath, String runtime) {
        Path path = Paths.get(zipFilePath);

        try {
            SdkBytes layerCode = SdkBytes.fromByteArray(Files.readAllBytes(path));

            PublishLayerVersionRequest request = PublishLayerVersionRequest.builder()
                    .layerName(layerName)
                    .description("Lambda Layer for " + layerName)
                    .content(LayerVersionContentInput.builder()
                            .zipFile(layerCode)
                            .build())
                    .compatibleRuntimesWithStrings(runtime)
                    .build();

            PublishLayerVersionResponse response = lambdaClient.publishLayerVersion(request);

            return response.layerArn();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating Lambda Layer: " + e.getMessage(), e);
        }
    }

    /**
     * Completely deletes all versions of a Lambda Layer.
     *
     * @param layerName The name of the Lambda Layer.
     */
    public void deleteLayerCompletely(String layerName) {
        try {
            // List all versions of the layer
            ListLayerVersionsRequest listRequest = ListLayerVersionsRequest.builder()
                    .layerName(layerName)
                    .build();

            ListLayerVersionsResponse listResponse = lambdaClient.listLayerVersions(listRequest);
            List<LayerVersionsListItem> versions = listResponse.layerVersions();

            // Delete each version of the layer
            for (LayerVersionsListItem version : versions) {
                deleteLayerVersion(layerName, version.version());
            }

        } catch (LambdaException e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting Lambda Layer versions: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a specific version of a Lambda Layer.
     *
     * @param layerName     The name of the Lambda Layer.
     * @param versionNumber The version number of the Lambda Layer to delete.
     */
    private void deleteLayerVersion(String layerName, long versionNumber) {
        try {
            DeleteLayerVersionRequest request = DeleteLayerVersionRequest.builder()
                    .layerName(layerName)
                    .versionNumber(versionNumber)
                    .build();

            lambdaClient.deleteLayerVersion(request);
            System.out.println("Deleted layer version " + versionNumber + " of layer " + layerName);

        } catch (LambdaException e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting Lambda Layer version: " + e.getMessage(), e);
        }
    }

    public List<String> listLambdaFunctionsByPrefix(String prefix) {
        ListFunctionsResponse response = lambdaClient.listFunctions();
        return response.functions().stream()
                .filter(f -> f.functionName().startsWith(prefix))
                .map(FunctionConfiguration::functionName)
                .collect(Collectors.toList());
    }

    public List<String> listLayersByPrefix(String prefix) {
        ListLayersResponse response = lambdaClient.listLayers();
        return response.layers().stream()
                .filter(f -> f.layerName().startsWith(prefix))
                .map(LayersListItem::layerName)
                .collect(Collectors.toList());
    }
}
