package io.github.raphaelmayer.models;

import java.util.List;

public class Ontology {

    public List<ServiceFunction> functions;
    public List<ServiceProvider> providers;

    public ServiceFunction getFunction(String functionName) {
        for (ServiceFunction f : functions) {
            if (f.name.equals(functionName)) {
                return f;
            }
        }

        return null;
    }

}
