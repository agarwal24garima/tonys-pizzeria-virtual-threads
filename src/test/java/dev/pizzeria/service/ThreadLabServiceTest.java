package dev.pizzeria.service;

import dev.pizzeria.model.ThreadReport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadLabServiceTest {

    private final ThreadLabService lab = new ThreadLabService();

    @Test
    void tenThousandVirtualThreadsAddAlmostNoOperatingSystemThreads() throws Exception {
        ThreadReport report = lab.start(true, 10_000);

        assertThat(report.started()).isEqualTo(10_000);
        // At most one carrier thread per core, plus a few helpers
        assertThat(report.extraOsThreads()).isLessThan(Runtime.getRuntime().availableProcessors() + 20);
    }

    @Test
    void everyPlatformThreadIsAnOperatingSystemThread() throws Exception {
        ThreadReport report = lab.start(false, 200);

        assertThat(report.started()).isEqualTo(200);
        assertThat(report.extraOsThreads()).isGreaterThanOrEqualTo(190);
    }
}
