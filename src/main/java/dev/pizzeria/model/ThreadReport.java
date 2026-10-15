package dev.pizzeria.model;

/**
 * What it cost to start many threads at once.
 *
 * @param kind           "platform" or "virtual"
 * @param requested      how many threads were asked for
 * @param started        how many actually started
 * @param millisToStart  how long it took to start them
 * @param extraOsThreads how many operating-system threads were added while they all existed
 * @param extraHeapMb    how much heap they used while they all existed (virtual threads keep their stacks there)
 * @param note           "All started.", or what the operating system said when it refused
 */
public record ThreadReport(String kind,
                           int requested,
                           int started,
                           long millisToStart,
                           int extraOsThreads,
                           long extraHeapMb,
                           String note) {
}
