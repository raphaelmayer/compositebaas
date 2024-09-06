package io.github.raphaelmayer.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.raphaelmayer.models.AfclBaseFunction;
import io.github.raphaelmayer.models.AfclDataInOut;
import io.github.raphaelmayer.models.AfclParallelFor;
import io.github.raphaelmayer.models.DataInOut;
import io.github.raphaelmayer.models.FunctionChoreography;
import io.github.raphaelmayer.models.Ontology;
import io.github.raphaelmayer.models.Resource;
import io.github.raphaelmayer.models.ServiceFunction;
import io.github.raphaelmayer.models.Transformation;
import io.github.raphaelmayer.models.TypeMapping;
import io.github.raphaelmayer.util.JsonUtils;
import io.github.raphaelmayer.util.Utils;
import io.github.raphaelmayer.util.YamlConfig;

public class FcGenerationService {

    // Pool to store available data (from dataIns and previous dataOuts)
    private Map<String, AfclDataInOut> availableData = new HashMap<>();
    private final Ontology ontology;

    public FcGenerationService(Ontology ontology) {
        this.ontology = ontology;
    }

    public FunctionChoreography generateFunctionChoreography(String fcName, List<ServiceFunction> servicePath,
            Transformation transformation) {
        FunctionChoreography fc = new FunctionChoreography(fcName);

        // add dynamic input data to FC dataIns
        addDynamicDataFromInputFile(fc, transformation);

        // Generate workflow body and connect functions
        for (ServiceFunction function : servicePath) {
            AfclBaseFunction afclFunction = new AfclBaseFunction(function.name, function.type);

            // Handle dataIns: Check if it's from fc.dataIns or a previous function's
            // dataOut
            for (DataInOut din : function.dataIns) {
                String source = findDataSource(din.name);
                if (source == null) {
                    if (din.required) {
                        System.out.println(availableData);
                        throw new RuntimeException("No source found for input: " + din.name);
                    }
                } else {
                    afclFunction.dataIns.add(new AfclDataInOut(din.name, din.type, source));
                }
            }

            // Handle dataOuts: Add function's outputs to the available data pool
            for (DataInOut dout : function.dataOuts) {
                afclFunction.dataOuts.add(new AfclDataInOut(dout.name, dout.type));
                availableData.put(dout.name,
                        new AfclDataInOut(dout.name, dout.type, afclFunction.name + "/" + dout.name));
            }

            fc.workflowBody.add(afclFunction);
        }

        // if a fc.dataIn is not used, apollo throws an IllegalStateException, because a
        // node is "disconnected". This should not matter, but I currently put all input
        // params into the fc.dataIns, which potentially results in multiple
        // disconnected nodes.
        // The workaround is, that the analyse function receives all input parameters.
        // Note: This results in duplicated analyse.dataIns in the yaml file.
        for (AfclDataInOut dio : fc.dataIns) {
            fc.workflowBody.get(0).dataIns.add(new AfclDataInOut(dio.name, dio.type, fc.name + '/' + dio.name));
        }

        AfclParallelFor pFor = new AfclParallelFor("TestLoop");
        pFor.iterators.add("SomeIterator");
        pFor.loopBody.add(new AfclBaseFunction("InLoop", "TEST"));
        // fc.workflowBody.add(pFor);

        // populate fc.dataOuts
        // we check the name of the DataOuts of the first function (analyse) to infer
        // the name here
        String dataFieldName = fc.workflowBody.get(0).dataOuts.get(0).name;
        String dataFieldType = fc.workflowBody.get(0).dataOuts.get(0).type;
        fc.dataOuts.add(new AfclDataInOut(dataFieldName, dataFieldType, availableData.get(dataFieldName).source));

        saveFunctionChoreography(fc);
        return fc;
    }

    /**
     * This method processes the input and output parameters from the transformation
     * object and adds them dynamically to the FunctionChoreography's dataIns. It
     * also prefixes the parameter keys with "input" or "output", capitalizes the
     * first letter of each key, and stores the data in the available data pool for
     * later use by other functions.
     *
     * @param fc             The FunctionChoreography object
     * @param transformation The Transformation object containing the data.
     */
    private void addDynamicDataFromInputFile(FunctionChoreography fc, Transformation transformation) {
        // Process input parameters
        processTransformationData(fc, transformation.input, "input");

        // Process output parameters
        processTransformationData(fc, transformation.output, "output");
    }

    /**
     * Helper function for addDynamicDataFromInputFile.
     *
     * @param fc      The FunctionChoreography object
     * @param dataMap The map of input/output data to process.
     * @param prefix  The prefix to be added to the key ("input" or "output").
     */
    private void processTransformationData(FunctionChoreography fc, Map<String, Object> dataMap, String prefix) {
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String valueType = determineType(value); // Helper function to determine the type

            // Add prefix (input/output) and capitalize the first letter of the key
            String formattedKey = prefix + Utils.capitalize(key);

            // Add to dataIns of the FunctionChoreography
            // fc.dataIns.add(new AfclDataInOut(formattedKey, valueType, value.toString()));
            fc.dataIns.add(new AfclDataInOut(formattedKey, valueType, formattedKey));

            // Add to available data pool
            availableData.put(formattedKey, new AfclDataInOut(formattedKey, valueType, fc.name + '/' + formattedKey));
        }
    }

    // Function to find the source of a data input
    private String findDataSource(String dataInName) {
        // Check if data is in availableData
        if (availableData.containsKey(dataInName)) {
            return availableData.get(dataInName).source;
        }
        return null; // Data not found
    }

    // Helper function to determine the type of input value dynamically
    private String determineType(Object value) {
        if (value instanceof String) {
            return "string";
        } else if (value instanceof Integer || value instanceof Double || value instanceof Float) {
            return "number";
        } else if (value instanceof Boolean) {
            return "boolean";
        } else if (value instanceof Map) {
            return "object";
        } else if (value instanceof List) {
            return "collection";
        } else {
            return "string"; // Default to string if unsure
        }
    }

    private void saveFunctionChoreography(FunctionChoreography fc) {
        ObjectMapper yamlMapper = YamlConfig.getYamlMapper();
        String fcFileName = fc.name + ".yaml";

        try {
            yamlMapper.writeValue(new File(fcFileName), fc);
            System.out.println("YAML file created: " + fcFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createTypeMappingsFile(String filePath, List<ServiceFunction> servicePath, List<String> functionUrls) {
        List<TypeMapping> typeMappings = new ArrayList<>();

        // Assuming each inner List corresponds to a function
        for (int i = 0; i < servicePath.size(); i++) {
            String fType = servicePath.get(i).type;

            Resource r = new Resource("Serverless", functionUrls.get(i));
            TypeMapping tm = new TypeMapping(fType, List.of(r));

            typeMappings.add(tm);
        }

        // Write the TypeMapping objects to a JSON file
        JsonUtils.writeFile(JsonUtils.convertToJsonNode(typeMappings), filePath);
    }

    public void createApolloInputFile(String inputFile, String outputFile) {
        JsonUtils.flattenFile(inputFile, outputFile);
    }

}
