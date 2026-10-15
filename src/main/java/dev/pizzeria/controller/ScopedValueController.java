package dev.pizzeria.controller;

import dev.pizzeria.model.Bill;
import dev.pizzeria.service.ScopedValueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 5. The coupon left on the counter. Open these two in order, then do the same with /coupon/scoped-value:
 *   http://localhost:8080/coupon/thread-local?customer=Rossi&coupon=HALF-PRICE
 *   http://localhost:8080/coupon/thread-local?customer=Bianchi
 */
@RestController
@RequestMapping("/coupon")
public class ScopedValueController {

    private final ScopedValueService service;

    public ScopedValueController(ScopedValueService service) {
        this.service = service;
    }

    @GetMapping("/thread-local")
    public Bill threadLocal(@RequestParam String customer,
                            @RequestParam(required = false) String coupon) throws Exception {
        return service.withThreadLocal(customer, coupon);
    }

    @GetMapping("/scoped-value")
    public Bill scopedValue(@RequestParam String customer,
                            @RequestParam(required = false) String coupon) throws Exception {
        return service.withScopedValue(customer, coupon);
    }
}
