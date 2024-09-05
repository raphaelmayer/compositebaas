package io.github.raphaelmayer.models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AfclParallelFor extends AfclBaseFunction {
    public List<String> iterators;
    public List<AfclBaseFunction> loopBody;

    public AfclParallelFor() {
        // Default constructor needed by Jackson
        super();
        this.iterators = new ArrayList<>();
        this.loopBody = new ArrayList<>();
    }

    public AfclParallelFor(String name) {
        super(name, "ParallelFor");
        iterators = new ArrayList<>();
        loopBody = new ArrayList<>();
    }

    public AfclParallelFor(String name, List<String> iterators, List<AfclBaseFunction> loopBody) {
        super(name, "ParallelFor");
        this.iterators = iterators;
        this.loopBody = loopBody;
    }
}
