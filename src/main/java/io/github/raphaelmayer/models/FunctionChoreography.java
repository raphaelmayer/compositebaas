package io.github.raphaelmayer.models;

import java.util.ArrayList;
import java.util.List;

public class FunctionChoreography {

    public String name;
    public List<AfclDataInOut> dataIns;
    public List<AfclBaseFunction> workflowBody;
    public List<AfclDataInOut> dataOuts;

    public FunctionChoreography(String name) {
        this.name = name;
        this.dataIns = new ArrayList<>();
        this.workflowBody = new ArrayList<>();
        this.dataOuts = new ArrayList<>();
    }

}
