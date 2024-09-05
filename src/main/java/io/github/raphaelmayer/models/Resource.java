package io.github.raphaelmayer.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Resource {
    private String type;
    private Map<String, Object> properties;

    private static final List<String> ALLOWED_TYPES = List.of("Serverless", "Local", "Demo");

    public Resource(String type, String link) {
        validateType(type);
        this.type = type;
        this.properties = new HashMap<>();

        // Set the appropriate property based on the resource type
        if (type.equals("Local")) {
            this.properties.put("Image", link);
        } else if (type.equals("Serverless")) {
            this.properties.put("Uri", link);
        } else if (type.equals("Demo")) {
            // no properties necessary
        } else {
            // unreachable, if validateType works correctly
            throw new IllegalArgumentException("Invalid resource type: " + type);
        }
    }

    // Full constructor
    public Resource(String type, Map<String, Object> properties) {
        validateType(type);
        this.type = type;
        this.properties = properties;
    }

    private void validateType(String type) {
        if (!ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid resource type: " + type);
        }
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        validateType(type);
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "type='" + type + '\'' +
                ", properties=" + properties +
                '}';
    }
}
