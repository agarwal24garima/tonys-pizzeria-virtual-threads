package dev.pizzeria.service;

import dev.pizzeria.model.Bill;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 5. The coupon left on the counter: ThreadLocal vs ScopedValue (final in Java 25, JEP 506).
 *
 * A customer may bring a coupon. The front desk puts it "on the counter", and the cashier,
 * deep inside checkout(), reads it from there. Nobody passes it as a parameter.
 *
 * There is one cashier: a pool with a single thread, so every customer meets the same thread.
 *
 *   ThreadLocal:  set() puts the coupon on the counter, and it stays there until someone calls
 *                 remove(). This code forgets, like real code often does. The next customer who
 *                 brings no coupon gets the last customer's discount.
 *   ScopedValue:  where(COUPON, coupon).call(...) puts it there only while call(...) runs.
 *                 When the block ends the counter is clean, even on a reused thread. There is
 *                 no set() and no remove() to forget.
 */
@Service
public class ScopedValueService {

    /** The old way: a value tied to the thread. */
    private static final ThreadLocal<String> COUPON_ON_COUNTER = new ThreadLocal<>();

    /** The Java 25 way: a value tied to a block of code. */
    private static final ScopedValue<String> COUPON = ScopedValue.newInstance();

    private static final double PIZZA_PRICE = 20.00;

    /** One cashier, so every customer is served by the same thread. */
    private final ExecutorService cashier = Executors.newSingleThreadExecutor(r -> new Thread(r, "cashier"));

    public Bill withThreadLocal(String customer, String coupon) throws Exception {
        return cashier.submit(() -> {
            if (coupon != null) {
                COUPON_ON_COUNTER.set(coupon);       // ...and nobody calls COUPON_ON_COUNTER.remove()
            }
            return checkout(customer, coupon != null, COUPON_ON_COUNTER.get());
        }).get();
    }

    public Bill withScopedValue(String customer, String coupon) throws Exception {
        return cashier.submit(() -> {
            if (coupon == null) {
                return checkout(customer, false, COUPON.isBound() ? COUPON.get() : null);   // not bound: no coupon
            }
            return ScopedValue.where(COUPON, coupon)    // bound only while call(...) runs
                    .call(() -> checkout(customer, true, COUPON.get()));
        }).get();
    }

    /** The till: it charges for whatever coupon was found on the counter. */
    private Bill checkout(String customer, boolean broughtCoupon, String couponFound) {
        double price = couponFound == null ? PIZZA_PRICE : PIZZA_PRICE / 2;
        String verdict = broughtCoupon || couponFound == null
                ? "Correct"
                : "Wrong: " + customer + " got a coupon an earlier customer left on the counter";
        return new Bill(customer, broughtCoupon, couponFound, String.format(Locale.ROOT, "%.2f", price),
                verdict, Thread.currentThread().getName());
    }
}
