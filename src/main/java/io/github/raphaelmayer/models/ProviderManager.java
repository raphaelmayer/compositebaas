package io.github.raphaelmayer.models;

import java.util.List;

public interface ProviderManager {

    public List<String> setupEnvironment(List<ServiceFunction> servicePaths);

    public void resetEnvironment();
}
