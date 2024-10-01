# CompositeBaaS

CompositeBaaS is ...

## Quickstart

### How to install

TODO

### General Usage

```
CompositeBaaS -f <path/to/inputfile> [--deploy] [--debug]     Generate workflow using the specified input file
CompositeBaaS --zip                                             Rezip all functions in the functions directory
CompositeBaaS --reset                                           Reset the cloud environment
CompositeBaaS -h | --help

Optional flags:
--deploy      Generate workflow and additionally set up the cloud environment and deploy all required functions.
--debug         Provide more output when running the program.
```

### Input File Requirements

CompositeBaaS requires an input file (in JSON) to define the data transformation process. The file should follow this general structure:

```json
{
    // general parameters

    "input": {},
    "output": {}
}
```

In the `input` and `output` sections, users can specify various parameters to describe the source and target data. Based on these parameters, CompositeBaaS will automatically identify and assemble a chain of services to generate a workflow that performs the required transformation.

**Currently available general parameters:**

-   `tbd` (String): To be determined...

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

    This JSON structure defines key properties like the function’s name, type, provider, and configuration, which CompositeBaaS uses to integrate the function into workflows. Attributes in the input object specify requirements for the particular service function, while outputs denote the particular transformation the service provides.

    To specify input and output arguments of your serverless function, use the dedicated dataIn and dataOut fields. (TODO)
    
    ```DataIn
    
                {
                    "name": "attributeName",
                    "type": "attributeType",
                    "required": boolean
                }
    ```
    
    ```DataOut
    
                {
                    "name": "attributeName",
                    "type": "attributeType"
                }
    ```

    CompositeBaaS will look for these attribute names when looking for services to apply for the specified intent/transformation. 

    *Note that attributes will be prefixed by their parent object from the input file. This means that a language attribute will be called inputLanguage or outputLanguage depending on where in the input file it was specified.*

2. **Implement the function as a serverless function:**
   Write the code for the serverless function and place it in the appropriate directory:  
   `resources/functions/<provider>/<name>.mjs`.

    CompositeBaaS will automatically detect and integrate the function based on this directory structure. For example you might want to place your custom functions in a directory `custom` within the `functions` directory. In this case use `"custom"` as a `provider` for your function.

    To implement a function use the template provided here (TODO) or copy the template from down below:
    ```
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

_Note: It is possible to implement functions in various different programming languages however layers expect "nodejs20x" as a runtime to work properly._

### Adding a layer

CompositeBaaS provides the following layers by default:

-   ffmpeg (nodejs20x)
-   ffprobe (nodejs20x)

To add a custom layer ...

---

## TODO

### Must Do / Critical:

-   **Split/Merge**
    -   mergeText currently receives an array of arrays due to how CompositeBaaS generates workflows. Each function works with multiple
    -   input files, this means the loop collects those collections and returns them as a collection of collections.
-   **Parallelize wf**
    -   we did parallelize, although hardcoded and not really pretty ✓
-   **handle lambda limitations** appropriately (file sizes, durations)
-   **regions**
    -   intelligent selection?
    -   option to set region? (CLI)
    -   fix hardcoded region in lambdas
-   **analyse**
    -   should filter fileNames result by type and format
-   **Language codes**:
    -   AWS uses the format xx-XX (almost?) everywhere. We use xx
    -   transcribe currently has inputLanguage hardcoded as "en-US"

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
