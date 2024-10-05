package com.microsoft.model;

import java.util.*;

public class ExecutionDag {
    private final List<List<Integer>> adjacencyList = new ArrayList<>();
    private final Map<Integer, IDagNode> nodeMap = new HashMap<>();
    private final Map<Integer, Integer> inDegree = new HashMap<>();

    private ExecutionDag() {

    }

    public static ExecutionDag create(Set<? extends INodeWithDependencies> nodes) {
        ExecutionDag dag = new ExecutionDag();

        // Initialize adjacency list
        int nodesSize = nodes.size();

        if(nodesSize == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("The graph is too big");
        }

        for (int i = 0; i < nodesSize; i++) {
            dag.adjacencyList.add(new ArrayList<>());
        }

        for (INodeWithDependencies node : nodes) {
            if (dag.nodeMap.get(node.id()) != null) {
                throw new IllegalArgumentException("The graph contains duplicate nodes");
            }

            dag.addNode(new DagNode(node.id()));

            for (INodeWithDependencies dependency : node.dependencies()) {
                int targetId = dependency.id();
                if(targetId >= dag.adjacencyList.size()) { // Check that the reference is valid
                    throw new IllegalArgumentException("The graph contains a reference to a non-existing node");
                }
                dag.addEdge(targetId, node.id()); // ⚠️ Edge direction is inverted
            }
        }

        if (dag.detectCycle()) {
            throw new IllegalArgumentException("The graph contains a cycle");
        }

        return dag;
    }

    private void addNode(IDagNode node) {
        nodeMap.put(node.id(), node);
        if(!inDegree.containsKey(node.id())) {
            inDegree.put(node.id(), 0);
        }
    }

    private void addEdge(int from, int to) {
        adjacencyList.get(from).add(to);
        inDegree.merge(to, 1, Integer::sum);
    }

    public IDagNode getNode(int id) {
        return nodeMap.get(id);
    }

    public List<List<Integer>> getAdjacencyList() {
        return adjacencyList;
    }

    public Map<Integer, Integer> getInDegree() {
        return inDegree;
    }

    private boolean detectCycle() {
        boolean[] visited = new boolean[nodeMap.size()];
        boolean[] recStack = new boolean[nodeMap.size()];

        for (int i = 0; i < nodeMap.size(); i++) {
            if (detectCycleUtil(i, visited, recStack)) {
                return true;
            }
        }

        return false;
    }

    private boolean detectCycleUtil(int i, boolean[] visited, boolean[] recStack) {
        if (recStack[i]) {
            return true;
        }

        if (visited[i]) {
            return false;
        }

        visited[i] = true;
        recStack[i] = true;

        for (int j : adjacencyList.get(i)) {
            if (detectCycleUtil(j, visited, recStack)) {
                return true;
            }
        }

        recStack[i] = false;

        return false;
    }
}
