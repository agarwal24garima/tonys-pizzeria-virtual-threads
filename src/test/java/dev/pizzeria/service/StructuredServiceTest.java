package dev.pizzeria.service;

import dev.pizzeria.model.KitchenReport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredServiceTest {

    /** Oven 500 ms, payment 250 ms, driver 250 ms, a declined card after 100 ms. */
    private final StructuredService service = new StructuredService(500, 250, 250, 100);

    @Test
    void bothServeAGoodOrderInAboutHalfASecond() throws Exception {
        for (KitchenReport report : new KitchenReport[] {
                service.withCompletableFuture(false), service.withStructuredTaskScope(false)}) {
            assertThat(report.outcome()).isEqualTo("Served");
            assertThat(report.millis()).isBetween(450L, 800L);
            assertThat(report.kitchen()).containsExactly("payment: paid", "oven: baked", "driver: booked");
        }
    }

    @Test
    void completableFutureLeavesTheOvenOn() {
        KitchenReport report = service.withCompletableFuture(true);

        assertThat(report.millis()).isLessThan(300);
        assertThat(report.kitchen()).containsExactly(
                "payment: declined", "oven: still working", "driver: still working");
    }

    @Test
    void structuredTaskScopeSwitchesItOff() throws Exception {
        KitchenReport report = service.withStructuredTaskScope(true);

        assertThat(report.millis()).isLessThan(300);
        assertThat(report.kitchen()).containsExactly(
                "payment: declined", "oven: switched off", "driver: called back");
    }
}
