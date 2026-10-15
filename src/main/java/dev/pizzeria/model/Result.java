package dev.pizzeria.model;

/**
 * What the scenario endpoints return. (The halftime rush returns a {@link Receipt}.)
 *
 * @param approach      how the work ran, for example "Virtual threads" or "synchronized"
 * @param detail        what was done, for example "kneaded 30000000 strokes"
 * @param thread        the thread that did the work
 * @param virtualThread true when that thread was a virtual thread
 * @param millis        time from the request arriving to the result, including any wait
 *                      for a thread, a lock or a database connection
 */
public record Result(String approach, String detail, String thread, boolean virtualThread, long millis) {

    /** Builds a result on the thread that did the work. */
    public static Result of(String approach, String detail, long arrivedNanos) {
        Thread current = Thread.currentThread();
        return new Result(approach, detail, current.toString(), current.isVirtual(),
                (System.nanoTime() - arrivedNanos) / 1_000_000);
    }
}
