package com.microsoft.execution;

import com.microsoft.model.IDagNode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class DagNodeExecutorTest {

    @Test
    public void testInvalidFailureRateNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DagNodeExecutor(4, -0.1f);
        });
    }

    @Test
    public void testInvalidFailureRateGreaterThanOne() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DagNodeExecutor(4, 1.1f);
        });
    }

    @Test
    public void testValidFailureRateZero() {
        assertDoesNotThrow(() -> {
            new DagNodeExecutor(4, 0.0f);
        });
    }

    @Test
    public void testValidFailureRateOne() {
        assertDoesNotThrow(() -> {
            new DagNodeExecutor(4, 1.0f);
        });
    }

    @Test
    public void testNegativeNumberOfEngines() {
        assertThrows(IllegalArgumentException.class, () -> {
            new DagNodeExecutor(-1, 0.5f);
        });
    }

    @Test
    public void testRequestFailWhenFailureRateIsOne() throws InterruptedException, ExecutionException {
        DagNodeExecutor dagNodeExecutor = new DagNodeExecutor(4, 1.0f);
        IDagNode mockNode = new IDagNode() {
            @Override
            public Integer id() {
                return 0;
            }

            @Override
            public void execute() {
                // Mock execution logic
            }
        };

        CompletableFuture<Integer> future = dagNodeExecutor.executeAsync(mockNode);
        int result = future.get();

        assertEquals(-1, result, "Expected execution to fail with result -1");
    }
}