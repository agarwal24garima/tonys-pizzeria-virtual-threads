package dev.pizzeria.service;

import dev.pizzeria.model.Result;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JEP 491 as a test. Run it on Java 21 and on Java 25: the same assertions hold on both,
 * because the expected result depends on the Java version.
 */
class PinningServiceTest {

    /** Every order waits 100 ms while it holds a lock. */
    private final PinningService pinning = new PinningService(100);

    /** Four orders per carrier thread: if they pin, they need four rounds. */
    private final int orders = 4 * Runtime.getRuntime().availableProcessors();

    @Test
    void synchronizedPinsOnJava21To23ButNotOnJava24AndLater() throws Exception {
        long millis = rush(pinning::withSynchronized);

        if (Runtime.version().feature() >= 24) {
            assertThat(millis).isLessThan(300);               // JEP 491: no pinning
        } else {
            assertThat(millis).isGreaterThanOrEqualTo(350);   // pinned: four rounds of 100 ms
        }
    }

    /** Sends all orders at the same moment on virtual threads and returns how long until the last one finished. */
    private long rush(Callable<Result> order) throws Exception {
        long start = System.nanoTime();
        try (ExecutorService customers = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Result>> results = new ArrayList<>();
            for (int i = 0; i < orders; i++) {
                results.add(customers.submit(order));
            }
            for (Future<Result> result : results) {
                result.get();
            }
        }
        return (System.nanoTime() - start) / 1_000_000;
    }
}
