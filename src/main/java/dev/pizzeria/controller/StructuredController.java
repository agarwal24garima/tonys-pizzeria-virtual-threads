package dev.pizzeria.controller;

import dev.pizzeria.model.KitchenReport;
import dev.pizzeria.service.StructuredService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 6. The oven nobody switched off. Watch the console while you open:
 *   http://localhost:8080/chef/completable-future?paymentFails=true
 *   http://localhost:8080/chef/structured?paymentFails=true
 */
@RestController
@RequestMapping("/chef")
public class StructuredController {

    private final StructuredService service;

    public StructuredController(StructuredService service) {
        this.service = service;
    }

    @GetMapping("/completable-future")
    public KitchenReport completableFuture(@RequestParam(defaultValue = "false") boolean paymentFails) {
        return service.withCompletableFuture(paymentFails);
    }

    @GetMapping("/structured")
    public KitchenReport structured(@RequestParam(defaultValue = "false") boolean paymentFails)
            throws InterruptedException {
        return service.withStructuredTaskScope(paymentFails);
    }
}
