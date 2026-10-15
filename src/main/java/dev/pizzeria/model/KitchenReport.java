package dev.pizzeria.model;

import java.util.List;

/**
 * What the customer hears back in demo 6, and what the kitchen was doing at that moment.
 *
 * @param approach "CompletableFuture" or "StructuredTaskScope"
 * @param order    the order number
 * @param outcome  "Served" or "Payment declined, order cancelled"
 * @param millis   time until the customer got this answer
 * @param kitchen  each task when the answer went out, for example "oven: still baking"
 */
public record KitchenReport(String approach,
                            int order,
                            String outcome,
                            long millis,
                            List<String> kitchen) {
}
