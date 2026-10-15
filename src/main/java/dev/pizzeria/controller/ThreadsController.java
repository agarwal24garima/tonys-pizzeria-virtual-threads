package dev.pizzeria.controller;

import dev.pizzeria.model.ThreadReport;
import dev.pizzeria.service.ThreadLabService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 2. What 10,000 threads cost. Try http://localhost:8080/threads/virtual?count=10000 */
@RestController
@RequestMapping("/threads")
public class ThreadsController {

    private final ThreadLabService threadLab;

    public ThreadsController(ThreadLabService threadLab) {
        this.threadLab = threadLab;
    }

    /** Capped at 20,000: platform threads are operating-system threads. */
    @GetMapping("/platform")
    public ThreadReport platform(@RequestParam(name = "count", defaultValue = "10000") int count) throws InterruptedException {
        return threadLab.start(false, Math.max(1, Math.min(count, 20_000)));
    }

    @GetMapping("/virtual")
    public ThreadReport virtual(@RequestParam(name = "count", defaultValue = "10000") int count) throws InterruptedException {
        return threadLab.start(true, Math.max(1, Math.min(count, 100_000)));
    }
}
