package io.github.raphaelmayer.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL) // DataOuts do not use the source field, i.e. it is null.
public class AfclDataInOut {
    public String name;
    public String type;
    public String source;

    public AfclDataInOut(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public AfclDataInOut(String name, String type, String source) {
        this.name = name;
        this.type = type;
        this.source = source;
    }

}
