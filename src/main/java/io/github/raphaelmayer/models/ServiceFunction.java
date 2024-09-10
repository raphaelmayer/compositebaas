package io.github.raphaelmayer.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.raphaelmayer.util.Utils;

public class ServiceFunction {

    public class FunctionLimits {
        // put in FunctionConfig?
        public String maxFileSize;
        public Integer rateLimit;
    }

    public class FunctionConfig {
        public Integer memory;
        public Integer timeout;
        public String runtime;
    }

    public ServiceFunction() {
        limits = new HashMap<>(); // new FunctionLimits(); // TODO
        input = new HashMap<>();
        output = new HashMap<>();
        regions = new ArrayList<>();
        dataIns = new ArrayList<>();
        dataOuts = new ArrayList<>();
        dependencies = new ArrayList<>();
        config = new FunctionConfig();
    }

    public String name;
    public String type;
    public String provider;
    public String description;
    public Map<String, Object> limits;
    public Map<String, Object> input;
    public Map<String, Object> output;
    public List<String> regions;
    public List<DataInOut> dataIns;
    public List<DataInOut> dataOuts;
    public List<String> dependencies;
    public FunctionConfig config;

    @Override
    public String toString() {
        return "ServiceFunction{" +
                provider.toUpperCase() + " " + Utils.capitalize(name) +
                '}';
    }

}
