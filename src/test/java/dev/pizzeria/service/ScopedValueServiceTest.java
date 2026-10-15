package dev.pizzeria.service;

import dev.pizzeria.model.Bill;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScopedValueServiceTest {

    private final ScopedValueService service = new ScopedValueService();

    @Test
    void threadLocalHandsTheNextCustomerTheLastCoupon() throws Exception {
        service.withThreadLocal("Rossi", "HALF-PRICE");
        Bill bianchi = service.withThreadLocal("Bianchi", null);

        assertThat(bianchi.couponUsed()).isEqualTo("HALF-PRICE");
        assertThat(bianchi.pays()).isEqualTo("10.00");
        assertThat(bianchi.verdict()).startsWith("Wrong");
    }

    @Test
    void scopedValueIsGoneWhenTheBlockEnds() throws Exception {
        Bill rossi = service.withScopedValue("Rossi", "HALF-PRICE");
        Bill bianchi = service.withScopedValue("Bianchi", null);

        assertThat(rossi.pays()).isEqualTo("10.00");
        assertThat(bianchi.couponUsed()).isNull();
        assertThat(bianchi.pays()).isEqualTo("20.00");
        assertThat(bianchi.thread()).isEqualTo(rossi.thread());   // same cashier, clean counter
    }
}
