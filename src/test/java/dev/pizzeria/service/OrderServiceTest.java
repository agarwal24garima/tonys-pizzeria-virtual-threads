package dev.pizzeria.service;

import dev.pizzeria.model.Order;
import dev.pizzeria.model.Receipt;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit tests, no Spring needed. A shorter kitchen keeps them fast:
 * bake 100 ms, payment 50 ms, driver 50 ms, so one order takes 200 ms step by step.
 */
class OrderServiceTest {

    private final KitchenService kitchen = new KitchenService(100, 50, 50);
    private final Order order = new Order("margherita", "Garima");

    @Test
    void traditionalCooksStepByStepOnAPlatformThread() throws Exception {
        Receipt receipt = new OrderService(kitchen, 2).traditional(order);

        assertThat(receipt.virtualThread()).isFalse();
        assertThat(receipt.millis()).isGreaterThanOrEqualTo(200);
        assertThat(receipt.pizza()).isEqualTo("margherita");
    }

    @Test
    void completableFutureRunsTheStepsAtTheSameTime() {
        Receipt receipt = new OrderService(kitchen, 2).completableFuture(order).join();

        assertThat(receipt.virtualThread()).isFalse();
        assertThat(receipt.millis()).isGreaterThanOrEqualTo(100).isLessThan(190);
    }

    @Test
    void virtualCooksStepByStepOnAVirtualThread() throws Exception {
        Receipt receipt = new OrderService(kitchen, 2).virtual(order);

        assertThat(receipt.virtualThread()).isTrue();
        assertThat(receipt.millis()).isGreaterThanOrEqualTo(200);
    }

    @Test
    void aRushQueuesForPlatformThreadsButNotForVirtualThreads() throws Exception {
        OrderService twoWaiters = new OrderService(kitchen, 2);

        // 10 customers at once. Two waiters need 5 rounds of 200 ms; virtual threads need one.
        long traditional = rush(10, () -> twoWaiters.traditional(order));
        long virtual = rush(10, () -> twoWaiters.virtual(order));

        assertThat(traditional).isGreaterThanOrEqualTo(900);
        assertThat(virtual).isLessThan(600);
    }

    /** Sends {@code customers} orders at the same moment and returns how long the slowest took. */
    private static long rush(int customers, Callable<Receipt> placeOrder) throws Exception {
        try (ExecutorService crowd = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Receipt>> receipts = new ArrayList<>();
            for (int i = 0; i < customers; i++) {
                receipts.add(crowd.submit(placeOrder));
            }
            long slowest = 0;
            for (Future<Receipt> receipt : receipts) {
                slowest = Math.max(slowest, receipt.get().millis());
            }
            return slowest;
        }
    }
}
