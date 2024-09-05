package io.github.raphaelmayer.models;

import java.util.ArrayList;
import java.util.List;

// TypeMapping class representing a single type mapping from a base function to a resource.
public class TypeMapping {
    private String functionType;
    private List<Resource> resources;

    // Constructor with resources list
    public TypeMapping(String functionType, List<Resource> resources) {
        this.functionType = functionType;
        this.resources = resources;
    }

    // Constructor with single resource
    public TypeMapping(String functionType, Resource resource) {
        this.functionType = functionType;
        this.resources = List.of(resource);
    }

    // Constructor that initializes resources as an empty list
    public TypeMapping(String functionType) {
        this.functionType = functionType;
        this.resources = new ArrayList<>(); // Initialize with an empty list
    }

    // Getters and Setters
    public String getFunctionType() {
        return functionType;
    }

    public void setFunctionType(String functionType) {
        this.functionType = functionType;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public void addResource(Resource resource) {
        this.getResources().add(resource);
    }

    @Override
    public String toString() {
        return "TypeMapping{" +
                "functionType='" + functionType + '\'' +
                ", resources=" + resources +
                '}';
    }
}
