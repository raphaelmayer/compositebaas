package io.github.raphaelmayer.providers.aws;

import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.regions.Region;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.raphaelmayer.util.Constants;
import io.github.raphaelmayer.util.Utils;
import io.github.raphaelmayer.models.ProviderManager;
import io.github.raphaelmayer.models.ServiceFunction;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

public class AwsManager implements ProviderManager {

    private final Region REGION = Region.US_EAST_1; // TODO: set region dynamically. lambdas also need region
                                                    // dynamically
    private final List<String> lambdaPolicies = List.of(
            "arn:aws:iam::aws:policy/AmazonS3FullAccess",
            "arn:aws:iam::aws:policy/AmazonTranscribeFullAccess",
            "arn:aws:iam::aws:policy/TranslateFullAccess");

    private final StsService sts;
    private final IamService iam;
    private final LambdaService lambda;
    private final ApiGatewayService apiGateway;

    public AwsManager() {
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create("default");

        StsClient stsClient = StsClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();
        IamClient iamClient = IamClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.AWS_GLOBAL)
                .build();
        LambdaClient lambdaClient = LambdaClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(this.REGION)
                .build();
        ApiGatewayClient apiGatewayClient = ApiGatewayClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(this.REGION)
                .build();

        this.sts = new StsService(stsClient);
        String accountId = this.sts.getAccountId();
        this.iam = new IamService(iamClient, lambdaPolicies);
        this.lambda = new LambdaService(lambdaClient, this.REGION.toString(), accountId);
        this.apiGateway = new ApiGatewayService(apiGatewayClient, this.REGION.toString());
    }

    @Override
    public List<String> setupEnvironment(List<ServiceFunction> servicePath) {
        List<String> functionUrls = new ArrayList<>();

        try {
            // Create the IAM role for Lambda
            String roleArn = iam.createAWSRole(Constants.LAMBDA_SERVICE_ROLE_NAME);
            System.out.println("Created role " + Constants.LAMBDA_SERVICE_ROLE_NAME + ": " + roleArn);

            // Create the API Gateway
            String apiId = apiGateway.createApiGateway(Constants.API_GATEWAY_NAME);
            System.out.println("API Gateway created with ID: " + apiId);

            // Create the Lambda layer
            String layerArn = lambda.createLayer(Constants.FFMPEG_LAYER_NAME,
                    Constants.FFMPEG_LAYER_ZIP_PATH, Constants.LAMBDA_RUNTIME);
            System.out.println("Created Lambda Layer ARN: " + layerArn);

            // For each service path, upload Lambda and create API integration
            for (ServiceFunction function : servicePath) {
                String functionUrl = deployLambda(function, roleArn, apiId);
                functionUrls.add(functionUrl);
            }

            // Deploy the API
            apiGateway.deployApi(apiId, Constants.API_GATEWAY_STAGE_NAME);
            System.out.println("API deployed to " + Constants.API_GATEWAY_STAGE_NAME + " stage");

        } catch (Exception e) {
            System.out.println("Failed to deploy services: " + e.getMessage());
            // TODO: Handle exception (logging, retrying, etc.)
        }

        return functionUrls;
    }

    @Override
    public void resetEnvironment() {
        // TODO: All those loops work, but it might be better to just check, if our
        // specific Gateway API, Role or Layer exists.
        // On the other hand is this very flexible.

        try {
            // Clean up Lambda functions
            List<String> lambdaFunctionNames = lambda
                    .listLambdaFunctionsByPrefix(Constants.LAMBDA_FUNCTION_PREFIX);
            for (String lambdaFunctionName : lambdaFunctionNames) {
                lambda.deleteLambda(lambdaFunctionName);
                System.out.println("Deleted Lambda function: " + lambdaFunctionName);
            }

            // Clean up all versions of a layer to delete it
            List<String> layerNames = lambda.listLayersByPrefix(Constants.LAMBDA_FUNCTION_PREFIX);
            for (String layerName : layerNames) {
                lambda.deleteLayerCompletely(layerName);
                System.out.println("Successfully deleted all versions of layer " + layerName);
            }

            // Clean up API Gateway
            List<String> apiGatewayIds = apiGateway.listApiGatewaysByName(Constants.API_GATEWAY_NAME);
            for (String apiId : apiGatewayIds) {
                apiGateway.deleteApiGateway(apiId);
                System.out.println("Deleted API Gateway: " + apiId);
            }

            // Clean up IAM roles (e.g., by prefix or name)
            List<String> roleNames = iam.listRolesByPrefix(Constants.LAMBDA_SERVICE_ROLE_NAME);
            for (String roleName : roleNames) {
                iam.deleteAWSRole(roleName);
                System.out.println("Deleted IAM role: " + roleName);
            }

            // Allow AWS time to propagate changes
            Utils.sleep(1000);

        } catch (Exception e) {
            System.err.println("Error occurred while resetting environment: " + e.getMessage());
            e.printStackTrace(); // TODO: Consider better logging or error handling
        }
    }

    public IamService getIam() {
        return iam;
    }

    public LambdaService getLambda() {
        return lambda;
    }

    public ApiGatewayService getApiGateway() {
        return apiGateway;
    }

    public StsService getSts() {
        return sts;
    }

    /**
     * Zip and upload a lambda function and expose it via an API Gateway. 
     * @param functionName
     * @param roleArn
     * @param apiId
     * @return
     */
    private String deployLambda(ServiceFunction function, String roleArn, String apiId) {
        String functionPath = Constants.FUNCTION_DIRECTORY + File.separator + function.provider + File.separator + function.name;
        String zipPath = Utils.zipFile(functionPath + ".js", functionPath + ".zip");

        String functionArn = lambda.uploadLambda(function.name, zipPath, roleArn);
        String functionUrl = exposeLambdaThroughApi(apiId, functionArn, function.name, "POST");
        System.out.println("Deployed function " + function.name + ": " + functionUrl);

        return functionUrl;
    }

    /**
     * Expose a Lambda function via API Gateway by creating a resource and
     * integrating the Lambda.
     * 
     * @param apiId        The API ID of the Gateway
     * @param lambdaArn    ARN of the Lambda function to expose
     * @param resourcePath API resource path (e.g., /functionname)
     * @param httpMethod   HTTP method (e.g., GET, POST)
     */
    private String exposeLambdaThroughApi(String apiId, String lambdaArn, String resourcePath, String httpMethod) {
        String rootResourceId = apiGateway.getRootResourceId(apiId);

        // Create a new resource under root (e.g., /functionname)
        String resourceId = apiGateway.createApiResource(apiId, rootResourceId, resourcePath);

        // Create an HTTP method (e.g., POST)
        apiGateway.createApiMethod(apiId, resourceId, httpMethod);

        // Integrate Lambda with API Gateway
        apiGateway.integrateApiWithLambda(apiId, resourceId, httpMethod, lambdaArn);

        // Add permissions for API Gateway to invoke the Lambda
        lambda.addLambdaInvokePermission(lambdaArn, apiId, resourcePath, httpMethod);

        // Construct and return the full API Gateway URL for the Lambda function
        String apiUrl = "https://" + apiId + ".execute-api." + REGION + ".amazonaws.com/"
                + Constants.API_GATEWAY_STAGE_NAME + "/" + resourcePath;
        System.out.println("Exposed Lambda URL: " + apiUrl);

        return apiUrl;
    }
}