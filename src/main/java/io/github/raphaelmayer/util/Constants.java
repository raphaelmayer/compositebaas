package io.github.raphaelmayer.util;

import java.io.File;

/**
 * A utility class to hold project-wide constants.
 * This class centralizes various configuration values such as directory paths,
 * Lambda function settings, AWS-related names, and runtime environments.
 */
public class Constants {

    // Directories
    // The directory where all resources are stored.
    public static final String RESOURCE_DIRECTORY = "src" + File.separator + "resources" + File.separator;
    // The directory where Lambda function source files are stored.
    public static final String FUNCTION_DIRECTORY = RESOURCE_DIRECTORY + "functions" + File.separator;
    // The directory where Lambda layers are stored.
    public static final String LAYER_DIRECTORY = RESOURCE_DIRECTORY + "layers" + File.separator;
    // The relative path of the ontology file.
    public static final String ONTOLOGY_PATH = RESOURCE_DIRECTORY + "function_ontology.json";

    // AWS Lambda Function Settings
    // Prefix for Lambda function names.
    public static final String LAMBDA_FUNCTION_PREFIX = "compositebaas-";
    // Name of the IAM role used by Lambda functions.
    public static final String LAMBDA_SERVICE_ROLE_NAME = "Lambda-Service-Role";
    // The runtime environment used by Lambda functions.
    public static final String LAMBDA_RUNTIME = "nodejs20.x";

    // AWS API Gateway Settings
    // Name of the API Gateway to expose Lambda functions.
    public static final String API_GATEWAY_NAME = "MultiLambdaPublicAPI";
    // The deployment stage name for the API Gateway.
    public static final String API_GATEWAY_STAGE_NAME = "prod";

    // AWS Lambda Layer Settings
    // Name of the custom Lambda layer containing ffmpeg.
    public static final String FFMPEG_LAYER_NAME = "ffmpeg-layer";
    // Path to the ZIP file containing the ffmpeg layer.
    public static final String FFMPEG_LAYER_ZIP_PATH = LAYER_DIRECTORY + FFMPEG_LAYER_NAME + ".zip";

    // Prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
