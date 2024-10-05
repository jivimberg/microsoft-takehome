package com.microsoft.parser;

import com.microsoft.model.ExecutionDag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DagParserTest {

    private final DagParser dagParser = new DagParser();

    @Test
    public void testParseValidDag() {
        String validDagXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies>
                            <Node Id="1"/>
                            <Node Id="2"/>
                        </dependencies>
                    </Node>
                    <Node Id="1">
                        <dependencies/>
                    </Node>
                    <Node Id="2">
                        <dependencies/>
                    </Node>
                </Nodes>
            </DAG>
        """;

        ExecutionDag dag = dagParser.parseDag(validDagXml);
        List<List<Integer>> adjacencyList = dag.getAdjacencyList();

        assertEquals(3, adjacencyList.size());

        assertTrue(adjacencyList.get(0).isEmpty());
        assertIterableEquals(adjacencyList.get(1), List.of(0));
        assertIterableEquals(adjacencyList.get(2), List.of(0));
    }

    @Test
    public void testParseInvalidDagXml() {
        String invalidDagXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies>
                            <Node Id="1"/>
                            <Node Id="2"/>
                        </dependencies>
                    </Node>
                    <Node Id="1">
                        <dependencies/>
                    </Node>
                    <Node Id="2">
                        <dependencies/>
                    </Node>
                </Nodes>
        """; // ⚠️ DAG tag is not closed
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dagParser.parseDag(invalidDagXml));
        assertEquals("Failed to parse DAG XML", exception.getMessage());
    }

    @Test
    public void testParseDagWithCycle() {
        String dagWithCycleXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies>
                            <Node Id="1"/>
                        </dependencies>
                    </Node>
                    <Node Id="1">
                        <dependencies>
                            <Node Id="0"/>
                        </dependencies>
                    </Node>
                </Nodes>
            </DAG>
        """;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dagParser.parseDag(dagWithCycleXml));
        assertEquals("The graph contains a cycle", exception.getMessage());
    }

    @Test
    public void testParseDagWithIslands() {
        String dagWithIslandsXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies>
                            <Node Id="1"/>
                        </dependencies>
                    </Node>
                    <Node Id="1">
                        <dependencies/>
                    </Node>
                    <Node Id="2">
                        <dependencies>
                            <Node Id="3"/>
                        </dependencies>
                    </Node>
                    <Node Id="3">
                        <dependencies/>
                    </Node>
                </Nodes>
            </DAG>
        """;

        ExecutionDag dag = dagParser.parseDag(dagWithIslandsXml);
        List<List<Integer>> adjacencyList = dag.getAdjacencyList();

        assertEquals(4, adjacencyList.size());

        assertTrue(adjacencyList.get(0).isEmpty());
        assertIterableEquals(adjacencyList.get(1), List.of(0));
        assertTrue(adjacencyList.get(2).isEmpty());
        assertIterableEquals(adjacencyList.get(3), List.of(2));
    }

    @Test
    public void testParseEmptyDag() {
        String emptyDagXml = """
            <DAG>
                <Nodes/>
            </DAG>
        """;

        ExecutionDag dag = dagParser.parseDag(emptyDagXml);
        List<List<Integer>> adjacencyList = dag.getAdjacencyList();

        assertTrue(adjacencyList.isEmpty());
    }

    @Test
    public void testParseSingleNodeNoDependencies() {
        String singleNodeXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies/>
                    </Node>
                </Nodes>
            </DAG>
        """;

        ExecutionDag dag = dagParser.parseDag(singleNodeXml);
        List<List<Integer>> adjacencyList = dag.getAdjacencyList();

        assertEquals(1, adjacencyList.size());
        assertTrue(adjacencyList.get(0).isEmpty());
    }

    @Test
    public void testParseSelfReferencingNode() {
        String selfReferencingNodeXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies>
                            <Node Id="0"/>
                        </dependencies>
                    </Node>
                </Nodes>
            </DAG>
        """;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dagParser.parseDag(selfReferencingNodeXml));
        assertEquals("The graph contains a cycle", exception.getMessage());
    }

    @Test
    public void testParseNonSequentialNodeIds() {
        String nonSequentialNodeIdsXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies>
                            <Node Id="2"/>
                        </dependencies>
                    </Node>
                    <Node Id="2">
                        <dependencies/>
                    </Node>
                </Nodes>
            </DAG>
        """;

        // Fail because the node with Id 1 is missing
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dagParser.parseDag(nonSequentialNodeIdsXml));
        assertEquals("The graph contains a reference to a non-existing node", exception.getMessage());
    }

    @Test
    public void testParseDuplicateNodeIds() {
        String duplicateNodeIdsXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies/>
                    </Node>
                    <Node Id="0">
                        <dependencies/>
                    </Node>
                </Nodes>
            </DAG>
        """;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dagParser.parseDag(duplicateNodeIdsXml));
        assertEquals("The graph contains duplicate nodes", exception.getMessage());
    }

    @Test
    public void testParseLargeDag() {
        StringBuilder largeDagXml = new StringBuilder("<DAG><Nodes>");
        for (int i = 0; i < 1000; i++) {
            largeDagXml.append("<Node Id=\"").append(i).append("\"><dependencies>");
            if (i > 0) {
                largeDagXml.append("<Node Id=\"").append(i - 1).append("\"/>");
            }
            largeDagXml.append("</dependencies></Node>");
        }
        largeDagXml.append("</Nodes></DAG>");

        System.out.println(largeDagXml);

        ExecutionDag dag = dagParser.parseDag(largeDagXml.toString());
        List<List<Integer>> adjacencyList = dag.getAdjacencyList();

        assertEquals(1000, adjacencyList.size());
        for (int i = 0; i < 1000; i++) {
            if (i == 999) {
                assertTrue(adjacencyList.get(i).isEmpty(), "The adjacency list for the first node should be empty");
            } else {
                assertIterableEquals(List.of(i + 1), adjacencyList.get(i), "The adjacency list for node " + i + " should contain " + (i + 1));
            }
        }
    }

    @Test
    public void testParseNonOrderedNodes() {
        String nonOrderedNodesXml = """
            <DAG>
                <Nodes>
                    <Node Id="2">
                        <dependencies>
                            <Node Id="3"/>
                        </dependencies>
                    </Node>
                    <Node Id="0">
                        <dependencies>
                            <Node Id="1"/>
                        </dependencies>
                    </Node>
                    <Node Id="3">
                        <dependencies/>
                    </Node>
                    <Node Id="1">
                        <dependencies/>
                    </Node>
                </Nodes>
            </DAG>
        """;

        ExecutionDag dag = dagParser.parseDag(nonOrderedNodesXml);
        List<List<Integer>> adjacencyList = dag.getAdjacencyList();

        assertEquals(4, adjacencyList.size());

        assertTrue(adjacencyList.get(0).isEmpty());
        assertIterableEquals(adjacencyList.get(1), List.of(0));
        assertTrue(adjacencyList.get(2).isEmpty());
        assertIterableEquals(adjacencyList.get(3), List.of(2));
    }

    @Test
    public void testParseNestedDependencies() {
        String nestedDependenciesXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies>
                            <Node Id="1">
                                <dependencies>
                                    <Node Id="2"/>
                                </dependencies>
                            </Node>
                        </dependencies>
                    </Node>
                    <Node Id="1">
                        <dependencies/>
                    </Node>
                    <Node Id="2">
                        <dependencies/>
                    </Node>
                </Nodes>
            </DAG>
        """;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dagParser.parseDag(nestedDependenciesXml));
        assertEquals("Failed to parse DAG XML", exception.getMessage());
    }

}