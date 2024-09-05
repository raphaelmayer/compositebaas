package io.github.raphaelmayer.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import io.github.raphaelmayer.models.Ontology;
import io.github.raphaelmayer.models.ServiceFunction;
import io.github.raphaelmayer.models.Transformation;

/*
 * Returns a List<ServiceFunction> instead of List<String>. This class is not in use right now.
 */
public class PathfindingServiceNew {

    private final Ontology ontology;

    public PathfindingServiceNew(Ontology ontology) {
        this.ontology = ontology;
    }

    /**
     * Find a valid sequence of service invocations (service path) to transform the
     * input state to the desired output state.
     *
     * @param transformation The transformation object with input and output states.
     * @return A list of ServiceFunction objects that represent the path to satisfy
     *         the
     *         transformation.
     */
    public List<ServiceFunction> findServicePath(Transformation transformation) {
        Map<String, Object> inputState = transformation.getInput();
        Map<String, Object> targetState = transformation.getOutput();

        // Perform BFS to find the service path
        List<ServiceFunction> servicePath = bfsServicePath(inputState, targetState);

        // Add the 'analyse' function to the beginning of the service path
        ServiceFunction analyseFunction = findServiceFunctionByName("analyse");
        if (analyseFunction != null) {
            servicePath.add(0, analyseFunction);
        }

        // Add split / merge functions if applicable
        // addSplitAndMergeFunctions(servicePath, inputState, targetState);

        return servicePath;
    }

    /**
     * Implements the BFS algorithm to find the shortest valid service path.
     *
     * @param inputState  The initial input state.
     * @param targetState The target output state.
     * @return The service path or an empty list if no path exists.
     */
    private List<ServiceFunction> bfsServicePath(Map<String, Object> inputState, Map<String, Object> targetState) {
        Queue<Node> queue = new LinkedList<>();
        Set<Map<String, Object>> visited = new HashSet<>();

        // Start the search from the initial state
        queue.add(new Node(inputState, new ArrayList<>()));

        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();
            Map<String, Object> currentState = currentNode.state;
            List<ServiceFunction> currentPath = currentNode.path;
            System.out.println("\n" + currentNode);

            // Check if the current state satisfies the target
            if (isTargetState(currentState, targetState)) {
                System.out.println("Found a valid path.");
                return currentPath; // Found a valid path!
            }

            // Avoid revisiting the same state
            if (visited.contains(currentState)) {
                continue;
            }

            visited.add(currentState);
            // Explore the services to find the next valid state transitions
            for (ServiceFunction service : ontology.functions) {
                // Apply the service and generate the next state if applicable
                if (canApplyService(service, currentState)) {
                    Map<String, Object> nextState = applyService(service, currentState, targetState);
                    System.out.println("Can apply " + service.name + ", new state: " + nextState);

                    // If this state has not been visited yet, enqueue it
                    if (!visited.contains(nextState)) {
                        List<ServiceFunction> newPath = new ArrayList<>(currentPath);
                        newPath.add(service); // Add the service function object to the path
                        queue.add(new Node(nextState, newPath));
                    }
                }
            }
        }

        return new ArrayList<>(); // No valid path found
    }

    /**
     * Check if the current state matches the target state. All keys in the target
     * state must match in the current state for this to be true.
     */
    private boolean isTargetState(Map<String, Object> currentState, Map<String, Object> targetState) {
        for (String key : targetState.keySet()) {
            if (!Objects.equals(currentState.get(key), targetState.get(key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines if a given service can be applied to the current state.
     */
    private boolean canApplyService(ServiceFunction service, Map<String, Object> currentState) {
        for (String key : service.input.keySet()) {
            // Ensure the current state matches the service's input requirements
            if (!currentState.containsKey(key)
                    || !((List<Object>) service.input.get(key)).contains(currentState.get(key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies a service to the current state and returns the resulting state.
     */
    private Map<String, Object> applyService(ServiceFunction service, Map<String, Object> currentState,
            Map<String, Object> targetState) {
        Map<String, Object> nextState = new HashMap<>(currentState);

        // Apply the transformation for each output key
        for (String key : service.output.keySet()) {
            // Update the current state with the service's output for this key
            List<Object> outputValues = (List<Object>) service.output.get(key);
            if (!outputValues.isEmpty()) {
                // if the service can transform in one step, apply the final value for the key
                if (outputValues.contains(targetState.get(key))) {
                    nextState.put(key, targetState.get(key));
                } else {
                    // Apply the service's first possible output for this key (we're moving towards
                    // target state incrementally)
                    nextState.put(key, outputValues.get(0));
                }
            }
        }

        return nextState;
    }

    /**
     * Helper method to find a service function by its name.
     */
    private ServiceFunction findServiceFunctionByName(String name) {
        for (ServiceFunction function : ontology.functions) {
            if (function.name.equalsIgnoreCase(name)) {
                return function;
            }
        }
        return null;
    }

    /**
     * Helper class to represent a node in the BFS traversal.
     */
    private static class Node {
        Map<String, Object> state;
        List<ServiceFunction> path;

        Node(Map<String, Object> state, List<ServiceFunction> path) {
            this.state = state;
            this.path = path;
        }

        @Override
        public int hashCode() {
            return state.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Node node = (Node) obj;
            return Objects.equals(state, node.state); // Equality by state content
        }

        @Override
        public String toString() {
            return "Node{" +
                    "state=" + state +
                    ", path=" + path +
                    '}';
        }
    }
}
