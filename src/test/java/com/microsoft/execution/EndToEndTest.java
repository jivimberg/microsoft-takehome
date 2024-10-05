package com.microsoft.execution;

import com.microsoft.execution.retry.*;
import com.microsoft.parser.DagParser;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

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
        DagNodeExecutor dagNodeExecutor = new DagNodeExecutor(4, 0.0f, NoRetryStrategy.INSTANCE);
        DagParser DagParser = new DagParser();
        IDagExecutor dagExecutor = new DagExecutor(DagParser, dagNodeExecutor);

        DagRequest request = new DagRequest(simpleDagXml);

        CompletableFuture<DagResponse> future = dagExecutor.processRequestAsync(request);

        DagResponse response = future.get();

        assertFalse(response.hasFailed());
    }

    @RepeatedTest(50)
    public void testMultipleDAGsExecution() throws ExecutionException, InterruptedException {
        testExecution(20, 10, 5, 4, 0.0f, NoRetryStrategy.INSTANCE);
    }

    @RepeatedTest(50)
    public void testMultipleDAGsExecutionWithFailure() throws ExecutionException, InterruptedException {
        testExecution(20, 10, 5, 4, 0.2f, NoRetryStrategy.INSTANCE);
    }

    @RepeatedTest(50)
    public void testMultipleLargeDAGsExecution() throws ExecutionException, InterruptedException {
        testExecution(20, 200, 10, 4, 0.0f, NoRetryStrategy.INSTANCE);
    }

    @RepeatedTest(50)
    public void testManyDAGsAtTheSameTime() throws ExecutionException, InterruptedException {
        testExecution(500, 10, 3, 4, 0.0f, NoRetryStrategy.INSTANCE);
    }

    @RepeatedTest(50)
    public void testSingleEngine() throws ExecutionException, InterruptedException {
        testExecution(20, 10, 5, 1, 0.0f, NoRetryStrategy.INSTANCE);
    }

    @RepeatedTest(50)
    public void testSingleEngineWithFailures() throws ExecutionException, InterruptedException {
        testExecution(20, 10, 5, 1, 0.5f, NoRetryStrategy.INSTANCE);
    }

    @RepeatedTest(50)
    public void testFailuresAndTimedRetryStrategy() throws ExecutionException, InterruptedException {
        TimedRetryStrategy retryStrategy = new TimedRetryStrategy(3, 10);
        testExecution(20, 10, 5, 4, 0.5f, retryStrategy);
    }

    @RepeatedTest(50)
    public void testFailuresAndInfiniteRetryStrategy() throws ExecutionException, InterruptedException {
        InfiteRetryStrategy retryStrategy = new InfiteRetryStrategy(10);
        testExecution(20, 10, 5, 4, 0.9f, retryStrategy);
    }

    @RepeatedTest(50)
    public void testFailuresAndExponentialBackOffStrategy() throws ExecutionException, InterruptedException {
        ExponentialBackoffRetryStrategy retryStrategy = new ExponentialBackoffRetryStrategy(3, 10, 2);
        testExecution(20, 10, 5, 4, 0.5f, retryStrategy);
    }

    @RepeatedTest(50)
    public void testEverythingFails() throws ExecutionException, InterruptedException {
        testExecution(20, 10, 5, 4, 1.0f, NoRetryStrategy.INSTANCE);
    }

    @RepeatedTest(50)
    public void testExecutionWithManyEngines() throws ExecutionException, InterruptedException {
        testExecution(50, 20, 4, 50, 0.0f, NoRetryStrategy.INSTANCE);
    }

    public void testExecution(int numDags, int numNodes, int maxEdges, int numberOfEngines, float failureRate, RetryStrategy retryStrategy) throws ExecutionException, InterruptedException {
        // Generate N random DAG XMLs
        List<String> dagXmls = IntStream.range(0, numDags)
                .mapToObj(_ -> generateRandomDAGXml(numNodes, maxEdges))
                .toList();

        DagParser dagParser = new DagParser();
        DagNodeExecutor dagNodeExecutor = new DagNodeExecutor(numberOfEngines, failureRate, retryStrategy);
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

        if (failureRate == 0 || retryStrategy instanceof InfiteRetryStrategy) {
            // Ensure all DAGs have completed successfully
            for (CompletableFuture<DagResponse> future : futures) {
                DagResponse response = future.get();
                assertFalse(response.hasFailed());
            }
        }

        if (failureRate == 1) {
            // Ensure all DAGs have completed successfully
            for (CompletableFuture<DagResponse> future : futures) {
                DagResponse response = future.get();
                assertTrue(response.hasFailed());
            }
        }
    }

    private static String generateRandomDAGXml(int numNodes, int maxEdges) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<DAG>\n  <Nodes>\n");

        Random random = new Random();
        Map<Integer, Set<Integer>> reachabilityMap = new HashMap<>();

        for (int i = 0; i < numNodes; i++) {
            xmlBuilder.append("    <Node Id=\"").append(i).append("\">\n");
            xmlBuilder.append("      <dependencies>\n");

            int edges = random.nextInt(maxEdges + 1);
            Set<Integer> dependencies = new HashSet<>();
            for (int j = 0; j < edges; j++) {
                int targetIndex = random.nextInt(numNodes);
                boolean createsCycle = reachabilityMap.containsKey(targetIndex) && reachabilityMap.get(targetIndex).contains(i);
                if (targetIndex != i && !createsCycle) {
                    dependencies.add(targetIndex);
                    updateReachabilityMap(reachabilityMap, i, targetIndex);
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

    private static void updateReachabilityMap(Map<Integer, Set<Integer>> reachabilityMap, int source, int target) {
        Set<Integer> reachableFromSource = reachabilityMap.computeIfAbsent(source, _ -> new HashSet<>());
        // Add the target to the reachability set of i
        reachableFromSource.add(target);
        // Add all the nodes reachable from the target to the reachability set of i
        reachableFromSource.addAll(reachabilityMap.getOrDefault(target, new HashSet<>()));

        reachabilityMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains(source))
                .forEach(entry -> entry.getValue().addAll(reachableFromSource));
    }
}