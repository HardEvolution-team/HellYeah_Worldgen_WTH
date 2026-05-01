package com.ded.genopt;

import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class GenerationProfiler {

    private static final Map<String, Long> START_TIMES = new HashMap<>();
    private static final Logger LOGGER = GenOptMod.LOGGER;

    public static void start(String task) {
        START_TIMES.put(task, System.nanoTime());
    }

    public static void end(String task) {
        Long startTime = START_TIMES.remove(task);
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            LOGGER.debug("Task {} took {} ms", task, duration / 1_000_000.0);
        }
    }
}
