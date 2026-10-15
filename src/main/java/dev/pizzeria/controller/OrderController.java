package dev.pizzeria.controller;

import dev.pizzeria.model.Order;
import dev.pizzeria.model.Receipt;
import dev.pizzeria.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Three doors into the same kitchen. Try them in a browser:
 * <pre>
 *   http://localhost:8080/orders/traditional
 *   http://localhost:8080/orders/completable-future
 *   http://localhost:8080/orders/virtual?pizza=diavola&amp;customer=Garima
 * </pre>
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 1. Traditional threads. */
    @GetMapping("/traditional")
    public Receipt traditional(@RequestParam(name = "pizza", defaultValue = "margherita") String pizza,
                               @RequestParam(name = "customer", defaultValue = "guest") String customer) throws Exception {
        return orderService.traditional(new Order(pizza, customer));
    }

    /** 2. CompletableFuture. Spring MVC sends the receipt when the future completes. */
    @GetMapping("/completable-future")
    public CompletableFuture<Receipt> completableFuture(@RequestParam(name = "pizza", defaultValue = "margherita") String pizza,
                                                        @RequestParam(name = "customer", defaultValue = "guest") String customer) {
        return orderService.completableFuture(new Order(pizza, customer));
    }

    /** 3. Virtual threads. */
    @GetMapping("/virtual")
    public Receipt virtual(@RequestParam(name = "pizza", defaultValue = "margherita") String pizza,
                           @RequestParam(name = "customer", defaultValue = "guest") String customer) throws Exception {
        return orderService.virtual(new Order(pizza, customer));
    }
}
