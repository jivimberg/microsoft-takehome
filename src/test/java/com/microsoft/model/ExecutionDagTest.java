package com.microsoft.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionDagTest {

    @Test
    public void testGetInDegreeValidDag() {
        NodeWithDependencies node2 = new NodeWithDependencies(2, Set.of());
        NodeWithDependencies node1 = new NodeWithDependencies(1, Set.of());
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of(node1, node2));

        ExecutionDag dag = ExecutionDag.create(Set.of(node0, node1, node2));
        Map<Integer, Integer> inDegree = dag.getInDegree();

        assertEquals(2, inDegree.get(0));
        assertEquals(0, inDegree.get(1));
        assertEquals(0, inDegree.get(2));
    }

    @Test
    public void testGetInDegreeWithIslands() {
        NodeWithDependencies node3 = new NodeWithDependencies(3, Set.of());
        NodeWithDependencies node2 = new NodeWithDependencies(2, Set.of(node3));
        NodeWithDependencies node1 = new NodeWithDependencies(1, Set.of());
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of(node1));

        ExecutionDag dag = ExecutionDag.create(Set.of(node0, node1, node2, node3));
        Map<Integer, Integer> inDegree = dag.getInDegree();

        assertEquals(1, inDegree.get(0));
        assertEquals(0, inDegree.get(1));
        assertEquals(1, inDegree.get(2));
        assertEquals(0, inDegree.get(3));
    }

    @Test
    public void testGetInDegreeSingleNode() {
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of());

        ExecutionDag dag = ExecutionDag.create(Set.of(node0));
        Map<Integer, Integer> inDegree = dag.getInDegree();

        assertEquals(0, inDegree.get(0));
    }

    @Test
    public void testGetInDegreeEmptyDag() {
        ExecutionDag dag = ExecutionDag.create(Set.of());
        Map<Integer, Integer> inDegree = dag.getInDegree();

        assertTrue(inDegree.isEmpty());
    }

    @Test
    public void testGetInDegreeMultipleDependencies() {
        NodeWithDependencies node3 = new NodeWithDependencies(3, Set.of());
        NodeWithDependencies node2 = new NodeWithDependencies(2, Set.of());
        NodeWithDependencies node1 = new NodeWithDependencies(1, Set.of());
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of(node1, node2, node3));

        ExecutionDag dag = ExecutionDag.create(Set.of(node0, node1, node2, node3));
        Map<Integer, Integer> inDegree = dag.getInDegree();

        assertEquals(3, inDegree.get(0));
        assertEquals(0, inDegree.get(1));
        assertEquals(0, inDegree.get(2));
        assertEquals(0, inDegree.get(3));
    }

    @Test
    public void testGetInDegreeDisconnectedNodes() {
        NodeWithDependencies node2 = new NodeWithDependencies(2, Set.of());
        NodeWithDependencies node1 = new NodeWithDependencies(1, Set.of());
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of());

        ExecutionDag dag = ExecutionDag.create(Set.of(node0, node1, node2));
        Map<Integer, Integer> inDegree = dag.getInDegree();

        assertEquals(0, inDegree.get(0));
        assertEquals(0, inDegree.get(1));
        assertEquals(0, inDegree.get(2));
    }

    @Test
    public void testGetInDegreeMultipleIncomingDependencies() {
        NodeWithDependencies node3 = new NodeWithDependencies(0, Set.of());
        NodeWithDependencies node2 = new NodeWithDependencies(1, Set.of(node3));
        NodeWithDependencies node1 = new NodeWithDependencies(2, Set.of(node3));
        NodeWithDependencies node0 = new NodeWithDependencies(3, Set.of(node1, node2));

        ExecutionDag dag = ExecutionDag.create(Set.of(node0, node1, node2, node3));
        Map<Integer, Integer> inDegree = dag.getInDegree();

        assertEquals(0, inDegree.get(0));
        assertEquals(1, inDegree.get(1));
        assertEquals(1, inDegree.get(2));
        assertEquals(2, inDegree.get(3));
    }

    @Test
    public void testDagWithCycle() {
        NodeWithDependencies node1 = new NodeWithDependencies(1, Set.of());
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of(node1));
        node1.setDependencies(Set.of(node0)); // Create a cycle

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ExecutionDag.create(Set.of(node0, node1)));
        assertEquals("The graph contains a cycle", exception.getMessage());
    }

    @Test
    public void testDagWithIslands() {
        NodeWithDependencies node3 = new NodeWithDependencies(3, Set.of());
        NodeWithDependencies node2 = new NodeWithDependencies(2, Set.of(node3));
        NodeWithDependencies node1 = new NodeWithDependencies(1, Set.of());
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of(node1));

        ExecutionDag dag = ExecutionDag.create(Set.of(node0, node1, node2, node3));
        Map<Integer, Integer> inDegree = dag.getInDegree();

        assertEquals(1, inDegree.get(0));
        assertEquals(0, inDegree.get(1));
        assertEquals(1, inDegree.get(2));
        assertEquals(0, inDegree.get(3));
    }

    @Test
    public void testEmptyDag() {
        ExecutionDag dag = ExecutionDag.create(Set.of());
        Map<Integer, Integer> inDegree = dag.getInDegree();

        assertTrue(inDegree.isEmpty());
    }

    @Test
    public void testSingleNodeNoDependencies() {
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of());

        ExecutionDag dag = ExecutionDag.create(Set.of(node0));
        Map<Integer, Integer> inDegree = dag.getInDegree();

        assertEquals(0, inDegree.get(0));
    }

    @Test
    public void testSelfReferencingNode() {
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of());
        node0.setDependencies(Set.of(node0)); // Self-referencing

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ExecutionDag.create(Set.of(node0)));
        assertEquals("The graph contains a cycle", exception.getMessage());
    }

    @Test
    public void testNonSequentialNodeIds() {
        NodeWithDependencies node2 = new NodeWithDependencies(2, Set.of());
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of(node2));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ExecutionDag.create(Set.of(node0, node2)));
        assertEquals("The graph contains a reference to a non-existing node", exception.getMessage());
    }

    @Test
    public void testDuplicateNodeIds() {
        NodeWithDependencies node0a = new NodeWithDependencies(0, Set.of());
        NodeWithDependencies node0b = new NodeWithDependencies(0, Set.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ExecutionDag.create(Set.of(node0a, node0b)));
        assertEquals("The graph contains duplicate nodes", exception.getMessage());
    }

    @Test
    public void testLargeDag() {
        Set<NodeWithDependencies> nodes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            Set<INodeWithDependencies> dependencies = i > 0 ? Set.of(new NodeWithDependencies(i - 1, Set.of())) : Set.of();
            nodes.add(new NodeWithDependencies(i, dependencies));
        }

        ExecutionDag dag = ExecutionDag.create(nodes);
        List<List<Integer>> adjacencyList = dag.getAdjacencyList();

        assertEquals(1000, adjacencyList.size());
        for (int i = 0; i < 1000; i++) {
            if (i == 999) {
                assertTrue(adjacencyList.get(i).isEmpty(), "The adjacency list for the last node should be empty");
            } else {
                assertIterableEquals(List.of(i + 1), adjacencyList.get(i), "The adjacency list for node " + i + " should contain " + (i + 1));
            }
        }
    }

    @Test
    public void testNonOrderedNodes() {
        NodeWithDependencies node3 = new NodeWithDependencies(3, Set.of());
        NodeWithDependencies node2 = new NodeWithDependencies(2, Set.of(node3));
        NodeWithDependencies node1 = new NodeWithDependencies(1, Set.of());
        NodeWithDependencies node0 = new NodeWithDependencies(0, Set.of(node1));

        ExecutionDag dag = ExecutionDag.create(Set.of(node0, node1, node2, node3));
        List<List<Integer>> adjacencyList = dag.getAdjacencyList();

        assertEquals(4, adjacencyList.size());
        assertTrue(adjacencyList.get(0).isEmpty());
        assertIterableEquals(List.of(0), adjacencyList.get(1));
        assertTrue(adjacencyList.get(2).isEmpty());
        assertIterableEquals(List.of(2), adjacencyList.get(3));
    }
}