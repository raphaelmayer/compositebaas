package io.github.raphaelmayer.models;

import java.util.List;
import java.util.Map;

import io.github.raphaelmayer.util.Utils;

public class ServiceFunction {

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

    @Override
    public String toString() {
        return provider.toUpperCase() + " " + Utils.capitalize(name);
    }

}
