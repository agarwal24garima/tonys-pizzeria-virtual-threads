package dev.pizzeria.service;

import dev.pizzeria.model.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 3. The dough station: CPU-bound work.
 *
 * Every order kneads dough: plain arithmetic that keeps one CPU core busy for about 50 ms
 * and never waits for anything. Virtual threads only help while a thread waits, so here they
 * bring no gain: both approaches are limited by the number of CPU cores.
 */
@Service
public class CpuService {

    private final long strokes;

    /** 20 platform threads, like a classic Tomcat. */
    private final ExecutorService platformThreads;

    public CpuService(@Value("${pizzeria.cpu.work:60000000}") long strokes,
                      @Value("${pizzeria.platform-threads:20}") int poolSize) {
        this.strokes = strokes;
        this.platformThreads = Executors.newFixedThreadPool(poolSize);
    }

    /** Traditional: the order waits for one of the 20 platform threads. */
    public Result traditional() throws Exception {
        long arrived = System.nanoTime();
        return platformThreads.submit(() -> makeDough("Traditional threads", arrived)).get();
    }

    /** Virtual: the request already runs on a virtual thread, so it just does the work. */
    public Result virtual() {
        return makeDough("Virtual threads", System.nanoTime());
    }

    private Result makeDough(String approach, long arrived) {
        long dough = kneadDough(strokes);
        return Result.of(approach, "kneaded " + strokes + " strokes (dough " + Long.toHexString(dough) + ")", arrived);
    }

    /** Kneads the dough: busy arithmetic, nothing to wait for. More strokes, more CPU time. */
    static long kneadDough(long strokes) {
        long dough = 1;
        for (long i = 0; i < strokes; i++) {
            dough = dough * 31 + i;     // one stroke
            dough ^= dough >>> 17;      // fold the dough
        }
        return dough;                   // returned, so the JVM can't skip the work
    }
}
