package com.microsoft.execution;

import com.microsoft.model.Dag;
import com.microsoft.model.IDagNode;
import com.microsoft.parser.DagParser;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class EndToEndTest {

    @Test
    public void testDagExecution() throws ExecutionException, InterruptedException {
        // Create a complex DAG XML
        String simpleDagXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies>
                            <Node Id="1"/>
                            <Node Id="2"/>
                        </dependencies>
                    </Node>
                    <Node Id="1">
                        <dependencies>
                            <Node Id="3"/>
                        </dependencies>
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

        // Configure the DagNodeExecutor with failure rate 0
        DagNodeExecutor dagNodeExecutor = new DagNodeExecutor(4, 0.0f);
        DagParser DagParser = new DagParser();
        IDagExecutor dagExecutor = new DagExecutor(DagParser, dagNodeExecutor);

        DagRequest request = new DagRequest(simpleDagXml);

        CompletableFuture<DagResponse> future = dagExecutor.processRequestAsync(request);

        DagResponse response = future.get();

        assertFalse(response.hasFailed());
    }

    @RepeatedTest(50)
    public void testMultipleDAGsExecution() throws ExecutionException, InterruptedException {
        testExecution(20, 10, 5, 4, 0.0f);
    }

    @RepeatedTest(50)
    public void testMultipleDAGsExecutionWithFailure() throws ExecutionException, InterruptedException {
        testExecution(20, 10, 5, 4, 0.2f);
    }

    @RepeatedTest(50)
    public void testMultipleLargeDAGsExecution() throws ExecutionException, InterruptedException {
        testExecution(20, 50, 2, 4, 0.0f);
    }

    @RepeatedTest(50)
    public void testManyDAGsAtTheSameTime() throws ExecutionException, InterruptedException {
        testExecution(500, 10, 3, 4, 0.0f);
    }

    @RepeatedTest(50)
    public void testSingleEngine() throws ExecutionException, InterruptedException {
        testExecution(20, 10, 5, 1, 0.0f);
    }

    @RepeatedTest(50)
    public void testSingleEngineWithFailures() throws ExecutionException, InterruptedException {
        testExecution(20, 10, 5, 1, 0.5f);
    }

    @RepeatedTest(50)
    public void testEverythingFails() throws ExecutionException, InterruptedException {
        testExecution(20, 10, 5, 1, 1.0f);
    }

    @RepeatedTest(50)
    public void testExecutionWithManyEngines() throws ExecutionException, InterruptedException {
        testExecution(50, 20, 4, 50, 0.0f);
    }

    public void testExecution(int numDags, int numNodes, int maxEdges, int numberOfEngines, float failureRate) throws ExecutionException, InterruptedException {
        // Generate N random DAG XMLs
        DagParser dagParser = new DagParser();
        List<String> dagXmls = IntStream.range(0, numDags)
                .mapToObj(_ -> generateValidRandomDAGXml(dagParser, numNodes, maxEdges))
                .toList();

        // Configure the DagNodeExecutor with failure rate 0
        DagNodeExecutor dagNodeExecutor = new DagNodeExecutor(numberOfEngines, failureRate);
        IDagExecutor dagExecutor = new DagExecutor(dagParser, dagNodeExecutor);

        // Submit each DAG for execution
        List<CompletableFuture<DagResponse>> futures = dagXmls.stream()
                .map(dagXml -> dagExecutor.processRequestAsync(new DagRequest(dagXml)))
                .toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        // Wait for all DAGs to complete
        await().until(() -> {
            allFutures.get();
            return true;
        });

        if(failureRate == 0) {
            // Ensure all DAGs have completed successfully
            for (CompletableFuture<DagResponse> future : futures) {
                DagResponse response = future.get();
                assertFalse(response.hasFailed());
            }
        }

        if(failureRate == 1) {
            // Ensure all DAGs have completed successfully
            for (CompletableFuture<DagResponse> future : futures) {
                DagResponse response = future.get();
                assertTrue(response.hasFailed());
            }
        }
    }

    private static String generateValidRandomDAGXml(DagParser dagParser, int numNodes, int maxEdges) {
        while (true) {
            String randomDagXml = generateRandomDAGXml(numNodes, maxEdges);
            try {
                dagParser.parseDag(randomDagXml); // If invalid parsing will throw
                return randomDagXml;
            } catch (Exception e) {
                System.out.println("Invalid DAG generated, retrying...");
            }
        }
    }

    private static String generateRandomDAGXml(int numNodes, int maxEdges) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<DAG>\n  <Nodes>\n");

        Random random = new Random();
        for (int i = 0; i < numNodes; i++) {
            xmlBuilder.append("    <Node Id=\"").append(i).append("\">\n");
            xmlBuilder.append("      <dependencies>\n");

            int edges = random.nextInt(maxEdges + 1);
            Set<Integer> dependencies = new HashSet<>();
            for (int j = 0; j < edges; j++) {
                int targetIndex = random.nextInt(numNodes);
                if (targetIndex != i) {
                    dependencies.add(targetIndex);
                }
            }

            for (Integer dep : dependencies) {
                xmlBuilder.append("        <Node Id=\"").append(dep).append("\"/>\n");
            }

            xmlBuilder.append("      </dependencies>\n");
            xmlBuilder.append("    </Node>\n");
        }

        xmlBuilder.append("  </Nodes>\n</DAG>");
        return xmlBuilder.toString();
    }

}