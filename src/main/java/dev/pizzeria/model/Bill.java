package dev.pizzeria.model;

/**
 * What the cashier hands over in demo 5.
 *
 * @param customer      who paid
 * @param broughtCoupon true when this customer brought a coupon
 * @param couponUsed    the coupon the cashier found and applied, or null
 * @param pays          the price after the coupon, for a 20.00 pizza
 * @param verdict       "Correct", or "Wrong" when the coupon belonged to someone else
 * @param thread        the thread that did the checkout (always the same one: there is one cashier)
 */
public record Bill(String customer,
                   boolean broughtCoupon,
                   String couponUsed,
                   String pays,
                   String verdict,
                   String thread) {
}
