package dev.pizzeria.service;

import dev.pizzeria.model.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 4. The stuck waiter: thread pinning.
 *
 * Every order waits 500 ms for the oven while it holds a lock. Each order has its own lock,
 * so orders never wait for each other: any slowdown you see is pinning, not contention.
 *
 *   Java 21 to 23:  a virtual thread that waits inside synchronized is stuck (pinned) to its
 *                   carrier thread. There is about one carrier per CPU core, so only that many orders
 *                   can wait at the same time.
 *   Java 24 and later (JEP 491), so Java 25 LTS: it unmounts like any other wait. No pinning.
 */
@Service
public class PinningService {

    private final Duration holdTime;

    public PinningService(@Value("${pizzeria.pinning.hold-ms:500}") long holdMs) {
        this.holdTime = Duration.ofMillis(holdMs);
    }

    public Result withSynchronized() throws InterruptedException {
        long arrived = System.nanoTime();
        new OvenWithSynchronized().waitForOven(holdTime);
        return Result.of("synchronized", heldFor(), arrived);
    }

    private String heldFor() {
        return "waited " + holdTime.toMillis() + " ms for the oven, holding a lock, on Java " + Runtime.version().feature();
    }

    /** Waits for the oven inside synchronized: pinned on Java 21. */
    static final class OvenWithSynchronized {
        synchronized void waitForOven(Duration time) throws InterruptedException {
            Thread.sleep(time);   // blocks while holding the monitor: pinned on Java 21, JFR points here
        }
    }
}
