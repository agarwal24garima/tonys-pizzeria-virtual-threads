package dev.pizzeria.service;

import dev.pizzeria.model.KitchenReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 6. The oven nobody switched off: CompletableFuture vs StructuredTaskScope
 * (a preview API in Java 25, JEP 505, so the app runs with --enable-preview).
 *
 * One order starts three tasks at once: bake (500 ms), take payment (250 ms) and book a driver
 * (250 ms). With ?paymentFails=true the card is declined after 100 ms.
 *
 *   CompletableFuture:   the customer hears "declined" after 100 ms, but the oven and the driver
 *                        keep going for nothing. Nothing stops them: even cancel(true) does not
 *                        interrupt a CompletableFuture's thread. Watch the console.
 *   StructuredTaskScope: the three tasks live inside one try block. When payment fails, the scope
 *                        cancels the other two, and the block does not end until they have stopped.
 */
@Service
public class StructuredService {

    private static final Logger log = LoggerFactory.getLogger(StructuredService.class);

    private final Duration ovenTime;
    private final Duration paymentTime;
    private final Duration driverTime;
    private final Duration declineTime;
    private final AtomicInteger orderNumbers = new AtomicInteger();
    private final ExecutorService kitchen = Executors.newVirtualThreadPerTaskExecutor();

    public StructuredService(@Value("${pizzeria.oven-ms:500}") long ovenMs,
                             @Value("${pizzeria.payment-ms:250}") long paymentMs,
                             @Value("${pizzeria.driver-ms:250}") long driverMs,
                             @Value("${pizzeria.decline-ms:100}") long declineMs) {
        this.ovenTime = Duration.ofMillis(ovenMs);
        this.paymentTime = Duration.ofMillis(paymentMs);
        this.driverTime = Duration.ofMillis(driverMs);
        this.declineTime = Duration.ofMillis(declineMs);
    }

    /** The usual way: three futures, then join them. */
    public KitchenReport withCompletableFuture(boolean paymentFails) {
        Ticket ticket = new Ticket(orderNumbers.incrementAndGet(), "CompletableFuture");

        var oven    = CompletableFuture.supplyAsync(() -> bake(ticket), kitchen);
        var payment = CompletableFuture.supplyAsync(() -> pay(ticket, paymentFails), kitchen);
        var driver  = CompletableFuture.supplyAsync(() -> bookDriver(ticket), kitchen);

        try {
            payment.join();
            oven.join();
            driver.join();
            return ticket.report("Served");
        } catch (CompletionException declined) {
            return ticket.cancel();                    // the oven and the driver are still running
        }
    }

    /** The Java 25 way: the three tasks belong to one scope. */
    public KitchenReport withStructuredTaskScope(boolean paymentFails) throws InterruptedException {
        Ticket ticket = new Ticket(orderNumbers.incrementAndGet(), "StructuredTaskScope");

        try (var scope = StructuredTaskScope.open()) {
            scope.fork(() -> bake(ticket));
            scope.fork(() -> pay(ticket, paymentFails));
            scope.fork(() -> bookDriver(ticket));

            scope.join();                              // waits for all three, or the first failure
            return ticket.report("Served");
        } catch (StructuredTaskScope.FailedException declined) {
            return ticket.cancel();                    // by now the other two have stopped
        }
    }

    // ── The three tasks ──────────────────────────────────────────────────────

    private String bake(Ticket ticket) {
        return ticket.work("oven", ovenTime, "baked", "switched off");
    }

    private String pay(Ticket ticket, boolean fails) {
        if (fails) {
            ticket.work("payment", declineTime, "declined", "stopped");
            throw new IllegalStateException("Card declined");
        }
        return ticket.work("payment", paymentTime, "paid", "stopped");
    }

    private String bookDriver(Ticket ticket) {
        return ticket.work("driver", driverTime, "booked", "called back");
    }

    /** One order: what each task is doing, and whether the customer has already had an answer. */
    private static final class Ticket {
        private final int number;
        private final String approach;
        private final long start = System.nanoTime();
        private final Map<String, String> status = new ConcurrentHashMap<>();
        private volatile long answeredAt;               // set when the customer was told "cancelled"

        Ticket(int number, String approach) {
            this.number = number;
            this.approach = approach;
        }

        String work(String task, Duration time, String done, String stopped) {
            status.put(task, "still working");
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                status.put(task, stopped);
                log.info("Order {} ({}): {} {}", number, approach, task, stopped);
                Thread.currentThread().interrupt();
                return stopped;
            }
            status.put(task, done);
            if (answeredAt != 0) {
                log.warn("Order {} ({}): {} {}, but the customer left {} ms ago", number, approach,
                        task, done, (System.nanoTime() - answeredAt) / 1_000_000);
            }
            return done;
        }

        KitchenReport cancel() {
            answeredAt = System.nanoTime();
            log.info("Order {} ({}): payment declined, customer told after {} ms", number, approach, millis());
            return report("Payment declined, order cancelled");
        }

        KitchenReport report(String outcome) {
            List<String> kitchen = List.of("payment", "oven", "driver").stream()
                    .map(task -> task + ": " + status.getOrDefault(task, "not started"))
                    .toList();
            return new KitchenReport(approach, number, outcome, millis(), kitchen);
        }

        private long millis() {
            return (System.nanoTime() - start) / 1_000_000;
        }
    }
}
