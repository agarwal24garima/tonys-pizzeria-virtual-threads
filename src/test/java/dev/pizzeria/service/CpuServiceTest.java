package dev.pizzeria.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpuServiceTest {

    @Test
    void kneadingIsRepeatable() {
        assertThat(CpuService.kneadDough(1_000)).isEqualTo(CpuService.kneadDough(1_000));
        assertThat(CpuService.kneadDough(1_000)).isNotEqualTo(CpuService.kneadDough(1_001));
    }

    @Test
    void moreStrokesTakeLonger() {
        CpuService.kneadDough(20_000_000);                       // warm up
        long small = time(10_000_000);
        long big = time(40_000_000);
        assertThat(big).isGreaterThan(small);
    }

    private static long time(long strokes) {
        long start = System.nanoTime();
        CpuService.kneadDough(strokes);
        return System.nanoTime() - start;
    }
}
