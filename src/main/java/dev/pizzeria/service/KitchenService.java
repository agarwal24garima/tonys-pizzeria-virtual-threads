package dev.pizzeria.service;

import dev.pizzeria.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * The slow parts of every order.
 *
 * Each method stands in for a call to another system: the oven is a database query,
 * the card machine is a payment API, the delivery app is an HTTP call to another service.
 * Thread.sleep blocks the calling thread exactly the way those calls would.
 */
@Service
public class KitchenService {

    private static final List<String> DRIVERS = List.of("Marco", "Sofia", "Luca", "Giulia", "Enzo");

    private final Duration ovenTime;
    private final Duration paymentTime;
    private final Duration driverTime;

    public KitchenService(@Value("${pizzeria.oven-ms:500}") long ovenMs,
                          @Value("${pizzeria.payment-ms:250}") long paymentMs,
                          @Value("${pizzeria.driver-ms:250}") long driverMs) {
        this.ovenTime = Duration.ofMillis(ovenMs);
        this.paymentTime = Duration.ofMillis(paymentMs);
        this.driverTime = Duration.ofMillis(driverMs);
    }

    /** Bake the pizza. Think: a database query. */
    public String bakePizza(Order order) {
        waitFor(ovenTime);
        return order.pizza();
    }

    /** Take the payment. Think: a call to a payment API. */
    public void takePayment(Order order) {
        waitFor(paymentTime);
    }

    /** Book a delivery driver. Think: an HTTP call to another service. */
    public String bookDriver(Order order) {
        waitFor(driverTime);
        return DRIVERS.get(Math.floorMod(order.customer().hashCode(), DRIVERS.size()));
    }

    private static void waitFor(Duration time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting", e);
        }
    }
}
