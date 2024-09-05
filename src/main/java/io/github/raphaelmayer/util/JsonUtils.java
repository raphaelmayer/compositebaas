package io.github.raphaelmayer.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class JsonUtils {

    public static class JsonParsingException extends RuntimeException {
        public JsonParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static <T> T parseFile(String filePath, Class<T> clazz) {
        System.out.println("Processing file: " + filePath);
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            return mapper.readValue(new File(filePath), clazz);
        } catch (IOException e) {
            throw new JsonParsingException("Error reading JSON file: " + filePath, e);
        }
    }

    public static <T> void writeFile(JsonNode data, String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            mapper.writeValue(new File(filePath), data);
            System.out.println("JSON file created: " + filePath);
        } catch (IOException e) {
            throw new JsonParsingException("Error writing JSON file: " + filePath, e);
        }
    }

    public static JsonNode convertToJsonNode(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(object);
    }

    public static void flattenFile(String inputFilePath, String outputFilePath) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.readTree(new File(inputFilePath));
            ObjectNode flattenedNode = mapper.createObjectNode();

            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode value = field.getValue();

                if (value.isObject()) {
                    flattenObject(key, value, flattenedNode);
                } else {
                    flattenedNode.set(key, value);
                }
            }

            JsonUtils.writeFile(flattenedNode, outputFilePath);

        } catch (IOException e) {
            throw new JsonParsingException("Error flattening JSON file: " + inputFilePath, e);
        }
    }

    private static void flattenObject(String prefix, JsonNode node, ObjectNode resultNode) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = prefix + Utils.capitalize(field.getKey());
            resultNode.set(key, field.getValue());
        }
    }
    
}
