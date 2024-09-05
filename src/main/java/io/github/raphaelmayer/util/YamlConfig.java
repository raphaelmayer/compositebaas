package io.github.raphaelmayer.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

public class YamlConfig {
    public static ObjectMapper getYamlMapper() {
        YAMLFactory factory = new YAMLFactory()
                // .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // Remove "---" at the start
                .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID); // Prevent use of !<Type> tags

        return new ObjectMapper(factory);
    }
}
