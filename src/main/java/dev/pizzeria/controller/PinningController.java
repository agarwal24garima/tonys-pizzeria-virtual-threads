package dev.pizzeria.controller;

import dev.pizzeria.model.Result;
import dev.pizzeria.service.PinningService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 4. Thread pinning. Both endpoints run on virtual threads (spring.threads.virtual.enabled=true).
 * Run the app on Java 21 and on Java 25 and compare /pinning/synchronized.
 */
@RestController
@RequestMapping("/pinning")
public class PinningController {

    private final PinningService pinningService;

    public PinningController(PinningService pinningService) {
        this.pinningService = pinningService;
    }

    @GetMapping("/synchronized")
    public Result withSynchronized() throws InterruptedException {
        return pinningService.withSynchronized();
    }

}
