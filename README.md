# CompositeBaaS

CompositeBaaS is a Java application designed to simplify the creation of workflows
across supported cloud services by leveraging an ontology-driven approach. This system
abstracts the implementation details of various cloud services, allowing users to only
specify their input and desired output without worrying about the technical aspects of
each service and how to orchestrate them. Serverless functions, wrapped in a flexible and
extendable ontology, serve as the building blocks of these workflows, while the Apollo
Runtime System handles the execution of the orchestration.

## Quickstart

### Requirements

- Java (tested with openjdk 11)
- Maven (tested with 3.9.6)
- git

If you want to automatically deploy workflows you also need to setup credentials for AWS. CompositeBaaS does not handle these, instead they are automatically picked up by the AWS SDK. Refer to the official instructions for additional information.

Additionally you will need to setup a user on AWS IAM with the necessary permissions to use the SDK. It might be possible to customize these, but I used the following permission policies provided by AWS for this user:    
- AmazonAPIGatewayAdministrator
- AmazonAPIGatewayInvokeFullAccess
- AWSCodeDeployRoleForLambda
- AWSLambda_FullAccess
- AWSLambdaRole
- IAMFullAccess
- stsGetCallerIdentity

If you also want to execute these workflows with Apollo, please refer to the official Apollo documentation [here](https://apollowf.github.io/learn.html) for instructions on how to setup and use Apollo.

### How to install

1. Clone the CompositeBaaS repository.
2. TODO

### General Usage

```
java -jar compositebaas.jar -f <path/to/input.json> -n <workflowName> [--deploy] [--debug] | Generate workflow using the specified input file
java -jar compositebaas.jar --zip | Rezip all functions in the functions directory
java -jar compositebaas.jar --reset <region> | Reset the specified cloud region
java -jar compositebaas.jar -h | --help | Display the help message

Optional flags:
--deploy      Generate workflow and additionally set up the cloud environment and deploy all required functions.
--debug         Provide more output when running the program.
```

### Input File Requirements

CompositeBaaS requires an input file (in JSON) to define the data transformation process. The file should follow this general structure:

```json
{
    "input": {},
    "output": {}
}
```

In the `input` and `output` sections, users can specify various parameters to describe the source and target data. Based on these parameters, CompositeBaaS will automatically identify and assemble a chain of services to generate a workflow that performs the required transformation.

**Currently available parameters for `input`:**

-   `bucket` (String): The name of the cloud storage bucket.
-   `fileNames` ([String]): A list of file names.
-   `type` ("video" | "audio" | "image" | "text"): The type of data.
-   `language` (ISO 639-1 two-letter code): A two-character code representing the language (e.g., "en").

**Currently available parameters for `output`:**

-   `bucket` (String): The name of the cloud storage bucket.
-   `type` ("video" | "audio" | "image" | "text"): The type of data.
-   `language` (ISO 639-1 two-letter code): A two-character code representing the language (e.g., "en").

## Customization

### Adding a Custom Function

To extend CompositeBaaS by adding a custom function, follow these steps:

1. **Add an entry to the ontology:**
   The ontology is stored as a JSON file in the resources directory and describes available functions and their configurations. To add a new function, include an entry similar to the example below:

    ```json
    {
        "name": "yourFunctionName",
        "type": "serviceType",
        "provider": "providerName",
        "description": "A brief description of the function",
        "limits": {
            "maxFileSize": "500MB",
            "rateLimit": 100
        },
        "input": {
            // Define the input parameters here
        },
        "output": {
            // Define the output parameters here
        },
        "regions": ["us-east-1", "eu-west-1"],
        "dataIns": [
            // Define input data requirements
        ],
        "dataOuts": [
            // Define output data requirements
        ],
        "dependencies": [
            // List any dependencies
        ],
        "config": {
            "memory": 256,
            "timeout": 60,
            "runtime": "nodejs20.x"
        }
    }
    ```

    This JSON structure defines key properties like the function’s name, type, provider, and configuration, which CompositeBaaS uses to integrate the function into workflows. Attributes in the input object specify **requirements** for the particular service function, while outputs denote the particular transformation the service provides. This means that usually you will need to introduce a new key to add new functionality. For example: Assume we want to add a video conversion service to CompositeBaaS, which can convert "mp4", "avi" or "mov" to "mp4", "avi" or "mov". We could specify this new transformation like this:
    
    ```json
    "input": {
        "format": ["mp4", "avi", "mov"]
    },
    "output": {
        "format": ["mp4", "avi", "mov"]
    }

    This service would now introduce a new parameter format, which users can use when specifying intents. CompositeBaaS automatically picks this new parameter up.

    To specify input arguments and return values of your serverless service function, use the dedicated dataIn and dataOut fields. (TODO)
    
    ```json
    DataIn
    {
        "name": "attributeName",
        "type": "attributeType",
        "required": "boolean"
    }
    ```
    
    ```json
    DataOut
    {
        "name": "attributeName",
        "type": "attributeType"
    }
    ```

    These fields are used during execution by Apollo to pass data. For more information on this, refer to the Apollo documentation [here](https://apollowf.github.io/learn.html).

    *Note that attributes will be prefixed by their parent key from the input file. This means that a language attribute will be passed to functions as inputLanguage or outputLanguage depending on where in the input file it was specified. DataIns/DataOuts need to be named accordingly.*

2. **Implement the function as a serverless function:**
   Write the code for the serverless function and place it in the appropriate directory:  
   `resources/functions/<provider>/<name>.mjs`.

    CompositeBaaS will automatically detect and integrate the function based on this directory structure. For example you might want to place your custom functions in a directory `custom` within the `functions` directory. In this case use `"custom"` as a `provider` for your function. 

    To implement a function use the template provided here (TODO) or copy the template from down below:
    ```javascript
    export const handler = async (event) => {
        try {
            const body = event.body ? JSON.parse(event.body) : event; // payload is different when triggering over APIGateway
            const fileNames = Array.isArray(body.fileNames) ? body.fileNames : [body.fileNames];
            const inputBucket = body.inputBucket;
            const outputBucket = body.outputBucket || inputBucket;

            // parse parameters here

            const outputKeys = [];

            for (const fileName of fileNames) {
                
                // implement function here

                const outputKey = ""; // save file in s3 and store outputKey here
                outputKeys.push(outputKey);
            }

            return {
                statusCode: 200,
                body: JSON.stringify({
                    fileNames: outputKeys,
                }),
            };
        } catch (error) {
            console.error("Error during my test service:", error);
            return {
                statusCode: error.statusCode || 500,
                body: error.message,
            };
        }
    };
    ```

    Your function should expect... (TODO)

If you want to see this in more detail, check out the resources directory, where you can find the `function_ontology.json` and the `functions` directory.

_Note: It is possible to implement functions in various different programming languages however layers currently expect "nodejs20x" as a runtime to work properly. Functions in other languages need to be packaged manually. For JS functions this happens automatically._
Please refer to the official documentation on how to prepare and package functions for uploading with the SDK.
 
### Adding a layer

CompositeBaaS provides the following layers by default:

-   ffmpeg (nodejs20x)
-   ffprobe (nodejs20x)

To add a custom layer refer to the AWS documentation to correctly set up and package it. The zip archive then needs to be placed into the resources/layers/<name> directory, where <name> is the layer name you can use to specify this dependency in the ontology. 

---

## TODO

### Must Do / Critical:

-   **Split/Merge**
    -   mergeText currently receives an array of arrays due to how CompositeBaaS generates workflows. Each function works with multiple
    -   input files, this means the loop collects those collections and returns them as a collection of collections.
    -   merge does not work. probably due to how the data is sent. 
-   **handle lambda limitations** appropriately (file sizes, durations)
-   **analyse**
    -   should filter fileNames result by type and format

### Important / High Priority:

-   **credentials**
-   **Directory structure and Output files**
    -   CompositeBaaS output files
    -   temp output workflow
    -   output workflow
    -   function directory structure
-   **tests**
-   **fileCount**
    - is it even necessary? (analyse and split functions)

### Optional / Low Priority:

-   **debug-flag**
    -   use it to enable / disable verbose console output
    - rename *verbose*?
-   **Comments**
    -   add comments (especially to models)
-   **Multi-Provider FCs**
    -   PathFindingService:
        -   check, which providers can be used (credentials present?)
        -   only consider functions, who's provider we can use
    -   DeploymentService:
        -   check, which providers we actually need
        -   set up each environment
        -   upload service functions to their respective providers
    -   FcGenerationService:
        -   none
    -   Misc:
        -   service path needs to contain more than just a function name
-   **generate config.xml**?
-   **service functions**
    -   We could potentially send text directly.
        **introduce document type**
    -   currently pdf is handled as an image. we could add a dedicated document type.
