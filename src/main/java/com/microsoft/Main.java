package com.microsoft;

import com.microsoft.execution.*;
import com.microsoft.parser.DagParser;
import com.microsoft.parser.IDagParser;

import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // Example of how to use the DAG executor.
        final IDagNodeExecutor dagNodeExecutor = new DagNodeExecutor(4, 0);
        final IDagParser dagParser = new DagParser();
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

        DagResponse dagResponse = dagExecutor.processRequestAsync(request).get();
        System.out.println("Dag response: " + dagResponse);
    }
}