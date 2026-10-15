package dev.pizzeria.model;

/**
 * What the customer gets back.
 *
 * @param thread        the thread that finished the order, for example
 *                      "Thread[#41,pool-1-thread-7,5,main]" or
 *                      "VirtualThread[#97]/runnable@ForkJoinPool-1-worker-3"
 * @param virtualThread true when that thread was a virtual thread
 * @param millis        time from the order arriving to the receipt, including any time
 *                      spent waiting for a free thread
 */
public record Receipt(long orderNumber,
                      String customer,
                      String pizza,
                      String driver,
                      String approach,
                      String thread,
                      boolean virtualThread,
                      long millis) {
}
