package dev.pizzeria.service;

import dev.pizzeria.model.ThreadReport;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 2. Ten thousand tickets: what a thread costs.
 *
 * Starts many threads that all wait until every one of them has started, then reports how long
 * that took, how many operating-system threads were added and how much heap was used.
 */
@Service
public class ThreadLabService {

    public ThreadReport start(boolean virtual, int count) throws InterruptedException {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long heapBefore = runtime.totalMemory() - runtime.freeMemory();
        int osThreadsBefore = threadBean.getThreadCount();   // counts platform threads only

        CountDownLatch release = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>(count);
        Thread.Builder builder = virtual ? Thread.ofVirtual() : Thread.ofPlatform();
        String note = "All started.";

        long t0 = System.nanoTime();
        try {
            for (int i = 0; i < count; i++) {
                threads.add(builder.start(() -> waitFor(release)));
            }
        } catch (OutOfMemoryError e) {
            note = "The operating system refused after " + threads.size() + " threads: " + e.getMessage();
        }
        long millis = (System.nanoTime() - t0) / 1_000_000;
        int osThreadsDuring = threadBean.getThreadCount();
        long heapDuring = runtime.totalMemory() - runtime.freeMemory();

        release.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
        return new ThreadReport(virtual ? "virtual" : "platform", count, threads.size(), millis,
                osThreadsDuring - osThreadsBefore, Math.max(0, (heapDuring - heapBefore) / (1024 * 1024)), note);
    }

    private static void waitFor(CountDownLatch release) {
        try {
            release.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
