package ssg.rwamp.feature.statistics;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WampStatisticsTest {

    @Test
    void counterGroupSnapshot() {
        var stats = new WampStatistics();
        var group = stats.group("realm1");

        group.call();
        group.call();
        group.result();
        group.error();

        var snapshot = group.snapshot();
        assertThat(snapshot).containsEntry("calls", 2L);
        assertThat(snapshot).containsEntry("results", 1L);
        assertThat(snapshot).containsEntry("errors", 1L);
        assertThat(snapshot).containsEntry("publishes", 0L);
    }

    @Test
    void multipleGroups() {
        var stats = new WampStatistics();
        stats.group("r1").call();
        stats.group("r1").call();
        stats.group("r2").publish();

        var snapshot = stats.snapshot();
        assertThat(snapshot).hasSize(2);
        assertThat(snapshot).containsKey("r1");
        assertThat(snapshot).containsKey("r2");
        var r1 = (Map<?, ?>) snapshot.get("r1");
        var r2 = (Map<?, ?>) snapshot.get("r2");
        assertThat(r1.get("calls")).isEqualTo(2L);
        assertThat(r2.get("publishes")).isEqualTo(1L);
    }

    @Test
    void reset() {
        var stats = new WampStatistics();
        var group = stats.group("realm1");

        group.call();
        group.publish();
        group.reset();

        var snapshot = group.snapshot();
        assertThat(snapshot.get("calls")).isEqualTo(0L);
        assertThat(snapshot.get("publishes")).isEqualTo(0L);
        assertThat(snapshot.get("results")).isEqualTo(0L);
    }

    @Test
    void threadSafety() throws InterruptedException {
        var stats = new WampStatistics();
        var group = stats.group("concurrent");

        var threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    group.call();
                }
            });
            threads[i].start();
        }

        for (var t : threads) t.join();

        assertThat(group.snapshot().get("calls")).isEqualTo(10_000L);
    }
}
