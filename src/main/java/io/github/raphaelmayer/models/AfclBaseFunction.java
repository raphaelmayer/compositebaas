package io.github.raphaelmayer.models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "_internalType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AfclBaseFunction.class, name = "function"),
        @JsonSubTypes.Type(value = AfclParallelFor.class, name = "parallelFor")
})
public class AfclBaseFunction {
    public String name;
    public String type;
    public List<AfclDataInOut> dataIns;
    public List<AfclDataInOut> dataOuts;

    public String _internalType; // for jackson to differentiate between AfclBaseFunction and compound functions.

    public AfclBaseFunction() {
        // Default constructor needed by Jackson
        this.dataIns = new ArrayList<>();
        this.dataOuts = new ArrayList<>();
    }

    public AfclBaseFunction(String name, String type) {
        this.name = name;
        this.type = type;
        this.dataIns = new ArrayList<>();
        this.dataOuts = new ArrayList<>();
    }
}
