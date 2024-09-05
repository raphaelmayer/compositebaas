package io.github.raphaelmayer.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Transformation {
    @JsonProperty("input")
    private Map<String, Object> input;
    @JsonProperty("output")
    private Map<String, Object> output;

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public void setOutput(Map<String, Object> output) {
        this.output = output;
    }

    @Override
    public String toString() {
        return "Transformation{" +
               "input=" + input +
               ", output=" + output +
               '}';
    }
}
