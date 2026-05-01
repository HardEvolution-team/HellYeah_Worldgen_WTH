package com.ded.genopt;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelWorkerPool {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    public static void submit(Runnable task) {
        EXECUTOR.submit(task);
    }

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }
}
