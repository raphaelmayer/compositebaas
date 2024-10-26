package io.github.raphaelmayer.models;

import java.util.Objects;

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

    @Override
    public String toString() {
        return "AfclDataInOut{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                (source != null ? ", source='" + source + '\'' : "") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AfclDataInOut that = (AfclDataInOut) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(type, that.type);
                // && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, source);
    }
}
