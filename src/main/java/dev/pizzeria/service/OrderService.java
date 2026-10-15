package dev.pizzeria.service;

import dev.pizzeria.model.Order;
import dev.pizzeria.model.Receipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One order, served three ways.
 *
 * Every order makes the same three slow calls: bake (500 ms), payment (250 ms)
 * and driver (250 ms). The only thing that changes is which threads do the work.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final KitchenService kitchen;
    private final AtomicLong orderNumbers = new AtomicLong();

    /** 1. Traditional: Tony's 20 waiters, a fixed pool of platform (operating-system) threads. */
    private final ExecutorService platformThreads;

    /** 2. CompletableFuture: the steps of each order run on a pool of the same size. */
    private final ExecutorService asyncPool;

    /** 3. Virtual threads: a new, cheap virtual thread for every order. No pool, no limit. */
    private final ExecutorService virtualThreads = Executors.newVirtualThreadPerTaskExecutor();

    public OrderService(KitchenService kitchen,
                        @Value("${pizzeria.platform-threads:20}") int poolSize) {
        this.kitchen = kitchen;
        this.platformThreads = Executors.newFixedThreadPool(poolSize);
        this.asyncPool = Executors.newFixedThreadPool(poolSize);
    }

    // ── 1. Traditional threads ──────────────────────────────────────────────────
    // One waiter takes the order and does every step, start to finish.
    // With 20 waiters, order 21 waits until one of them is free.
    public Receipt traditional(Order order) throws Exception {
        long arrived = System.nanoTime();
        return platformThreads.submit(() -> cookStepByStep(order, "Traditional threads", arrived)).get();
    }

    // ── 2. CompletableFuture ────────────────────────────────────────────────────
    // The three steps run at the same time, so one order takes 500 ms instead of 1 s.
    // But each step still holds a platform thread while it waits.
    public CompletableFuture<Receipt> completableFuture(Order order) {
        long arrived = System.nanoTime();
        CompletableFuture<String> pizza = CompletableFuture.supplyAsync(() -> kitchen.bakePizza(order), asyncPool);
        CompletableFuture<Void> payment = CompletableFuture.runAsync(() -> kitchen.takePayment(order), asyncPool);
        CompletableFuture<String> driver = CompletableFuture.supplyAsync(() -> kitchen.bookDriver(order), asyncPool);

        return CompletableFuture.allOf(pizza, payment, driver)
                .thenApply(done -> receipt(order, pizza.join(), driver.join(), "CompletableFuture", arrived));
    }

    // ── 3. Virtual threads ──────────────────────────────────────────────────────
    // Exactly the same code as traditional, on a virtual thread. While it waits,
    // the virtual thread steps off its carrier thread, so no order waits in line.
    public Receipt virtual(Order order) throws Exception {
        long arrived = System.nanoTime();
        return virtualThreads.submit(() -> cookStepByStep(order, "Virtual threads", arrived)).get();
    }

    /** Plain blocking code: one step after another. */
    private Receipt cookStepByStep(Order order, String approach, long arrived) {
        String pizza = kitchen.bakePizza(order);
        kitchen.takePayment(order);
        String driver = kitchen.bookDriver(order);
        return receipt(order, pizza, driver, approach, arrived);
    }

    private Receipt receipt(Order order, String pizza, String driver, String approach, long arrived) {
        Thread thread = Thread.currentThread();
        long millis = (System.nanoTime() - arrived) / 1_000_000;
        Receipt receipt = new Receipt(orderNumbers.incrementAndGet(), order.customer(), pizza, driver,
                approach, thread.toString(), thread.isVirtual(), millis);
        log.info("{} | order {} for {} | {} ms | {}", approach, receipt.orderNumber(), order.customer(), millis, thread);
        return receipt;
    }
}
