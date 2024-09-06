package io.github.raphaelmayer.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Transformation {
    @JsonProperty("input")
    public Map<String, Object> input;
    @JsonProperty("output")
    public Map<String, Object> output;

    @Override
    public String toString() {
        return "Transformation{" +
               "input=" + input +
               ", output=" + output +
               '}';
    }
}
