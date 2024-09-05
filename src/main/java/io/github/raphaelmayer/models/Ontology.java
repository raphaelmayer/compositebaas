package io.github.raphaelmayer.models;

import java.util.List;

public class Ontology {

    public List<ServiceFunction> functions;
    public List<ServiceProvider> providers;

    public List<ServiceFunction> getFunctions() {
        return functions;
    }

    public void setFunctions(List<ServiceFunction> functions) {
        this.functions = functions;
    }

    public List<ServiceProvider> getProviders() {
        return providers;
    }

    public void setProviders(List<ServiceProvider> providers) {
        this.providers = providers;
    }

    public ServiceFunction getFunction(String functionName) {
        for (ServiceFunction f : functions) {
            if (f.name.equals(functionName)) {
                return f;
            }
        }

        return null;
    }

}
