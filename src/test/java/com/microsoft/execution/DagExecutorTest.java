package com.microsoft.execution;

import com.microsoft.parser.DagParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class DagExecutorTest {

    private final DagParser dagParser = new DagParser();

    @Test
    public void testProcessRequestAsyncValidDag() throws ExecutionException, InterruptedException {
        final FakeDagNodeExecutor dagNodeExecutor = new FakeDagNodeExecutor(1);
        final IDagExecutor dagExecutor = new DagExecutor(dagParser, dagNodeExecutor);

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
        DagRequest request = new DagRequest(validDagXml);

        CompletableFuture<DagResponse> future = dagExecutor.processRequestAsync(request);
        dagNodeExecutor.process(3); // Simulate processing of 3 nodes

        DagResponse response = future.get();
        assertFalse(response.hasFailed());
    }

    @Test
    public void testProcessRequestAsyncWithFailureOnRoot() throws ExecutionException, InterruptedException {
        final Set<Integer> nodesThatWillFail = Set.of(1);
        final FakeDagNodeExecutor dagNodeExecutor = new FakeDagNodeExecutor(1, nodesThatWillFail);
        final IDagExecutor dagExecutor = new DagExecutor(dagParser, dagNodeExecutor);

        String validDagXml = """
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
                </Nodes>
            </DAG>
        """;
        DagRequest request = new DagRequest(validDagXml);

        CompletableFuture<DagResponse> future = dagExecutor.processRequestAsync(request);
        dagNodeExecutor.process(2);

        DagResponse response = future.get();
        assertTrue(response.hasFailed());
    }

    @Test
    public void testProcessRequestAsyncComplexGraph() throws ExecutionException, InterruptedException {
        final FakeDagNodeExecutor dagNodeExecutor = new FakeDagNodeExecutor(1);
        final IDagExecutor dagExecutor = new DagExecutor(dagParser, dagNodeExecutor);

        String complexDagXml = """
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
                        <dependencies>
                            <Node Id="4"/>
                        </dependencies>
                    </Node>
                    <Node Id="4">
                        <dependencies/>
                    </Node>
                </Nodes>
            </DAG>
        """;
        DagRequest request = new DagRequest(complexDagXml);

        CompletableFuture<DagResponse> future = dagExecutor.processRequestAsync(request);
        dagNodeExecutor.process(5); // Simulate processing of 5 nodes

        DagResponse response = future.get();
        assertFalse(response.hasFailed());

        List<Integer> expectedOrder1 = List.of(4, 3, 1, 2, 0);
        List<Integer> expectedOrder2 = List.of(4, 3, 2, 1, 0);
        List<Integer> actualExecution = dagNodeExecutor.getNodesExecuted();
        assertTrue(actualExecution.equals(expectedOrder1) || actualExecution.equals(expectedOrder2));
    }

    @Test
    public void testProcessRequestAsyncWithFailureOnInternalNode() throws ExecutionException, InterruptedException {
        final Set<Integer> nodesThatWillFail = Set.of(1);
        final FakeDagNodeExecutor dagNodeExecutor = new FakeDagNodeExecutor(1, nodesThatWillFail);
        final IDagExecutor dagExecutor = new DagExecutor(dagParser, dagNodeExecutor);

        String linearDagXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies>
                            <Node Id="1"/>
                        </dependencies>
                    </Node>
                    <Node Id="1">
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
        DagRequest request = new DagRequest(linearDagXml);

        CompletableFuture<DagResponse> future = dagExecutor.processRequestAsync(request);
        dagNodeExecutor.process(3); // Simulate processing of 3 nodes

        DagResponse response = future.get();
        assertTrue(response.hasFailed());

        // Validate that nodes 1 and 2 have been executed
        List<Integer> expectedOrder = List.of(2, 1);
        assertEquals(expectedOrder, dagNodeExecutor.getNodesExecuted());
    }

    @Test
    public void testProcessRequestAsyncWithFailureOnLeafNode() throws ExecutionException, InterruptedException {
        final Set<Integer> nodesThatWillFail = Set.of(0);
        final FakeDagNodeExecutor dagNodeExecutor = new FakeDagNodeExecutor(1, nodesThatWillFail);
        final IDagExecutor dagExecutor = new DagExecutor(dagParser, dagNodeExecutor);

        String linearDagXml = """
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
                </Nodes>
            </DAG>
        """;
        DagRequest request = new DagRequest(linearDagXml);

        CompletableFuture<DagResponse> future = dagExecutor.processRequestAsync(request);
        dagNodeExecutor.process(2);

        DagResponse response = future.get();
        assertTrue(response.hasFailed());

        List<Integer> expectedOrder = List.of(1, 0);
        assertEquals(expectedOrder, dagNodeExecutor.getNodesExecuted());
    }

    @Test
    public void testMultipleNodesCanBeExecutedInParallel() {
        final FakeDagNodeExecutor dagNodeExecutor = new FakeDagNodeExecutor(2);
        final IDagExecutor dagExecutor = new DagExecutor(dagParser, dagNodeExecutor);

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
        DagRequest request = new DagRequest(validDagXml);

        dagExecutor.processRequestAsync(request);

        await().until(() -> dagNodeExecutor.getThreadsWaiting() == 2);
    }

    @Test
    public void testMultipleDagsCanBeExecutedInParallel() throws ExecutionException, InterruptedException {
        final FakeDagNodeExecutor dagNodeExecutor = new FakeDagNodeExecutor(4);
        final IDagExecutor dagExecutor = new DagExecutor(dagParser, dagNodeExecutor);

        String dagXml = """
            <DAG>
                <Nodes>
                    <Node Id="0">
                        <dependencies/>
                    </Node>
                </Nodes>
            </DAG>
        """;

        DagRequest request1 = new DagRequest(dagXml);
        DagRequest request2 = new DagRequest(dagXml);

        CompletableFuture<DagResponse> future1 = dagExecutor.processRequestAsync(request1);
        CompletableFuture<DagResponse> future2 = dagExecutor.processRequestAsync(request2);

        await().until(() -> dagNodeExecutor.getThreadsWaiting() == 2); // Both dags are being executed in parallel

        dagNodeExecutor.process(2); // Simulate processing of all nodes

        DagResponse response1 = future1.get();
        DagResponse response2 = future2.get();

        assertFalse(response1.hasFailed());
        assertFalse(response2.hasFailed());
    }
}