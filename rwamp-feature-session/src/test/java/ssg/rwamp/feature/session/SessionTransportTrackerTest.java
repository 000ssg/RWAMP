package ssg.rwamp.feature.session;

import ssg.legoflow.wamp.core.transport.WampTransport;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTransportTrackerTest {

    @Test
    void trackAndGetTransport() {
        var tracker = new SessionTransportTracker();
        var transport = new InMemoryTransport();

        tracker.track(1L, transport);
        assertThat(tracker.getTransport(1L)).contains(transport);
        assertThat(tracker.getTransport(999L)).isEmpty();
    }

    @Test
    void untrackRemovesMapping() {
        var tracker = new SessionTransportTracker();
        var transport = new InMemoryTransport();

        tracker.track(1L, transport);
        assertThat(tracker.untrack(1L)).isTrue();
        assertThat(tracker.getTransport(1L)).isEmpty();
        assertThat(tracker.untrack(1L)).isFalse();
    }

    @Test
    void getActiveTransportsReturnsSnapshot() {
        var tracker = new SessionTransportTracker();
        var t1 = new InMemoryTransport();
        var t2 = new InMemoryTransport();

        tracker.track(1L, t1);
        tracker.track(2L, t2);

        var snapshot = tracker.getActiveTransports();
        assertThat(snapshot).hasSize(2);
        assertThat(snapshot).containsEntry(1L, t1);
        assertThat(snapshot).containsEntry(2L, t2);

        // Modify tracker after snapshot
        tracker.untrack(1L);
        assertThat(snapshot).hasSize(2); // snapshot is independent
        assertThat(tracker.getActiveTransports()).hasSize(1);
    }

    @Test
    void trackedCount() {
        var tracker = new SessionTransportTracker();
        assertThat(tracker.getTrackedCount()).isZero();

        tracker.track(1L, new InMemoryTransport());
        assertThat(tracker.getTrackedCount()).isOne();

        tracker.track(2L, new InMemoryTransport());
        assertThat(tracker.getTrackedCount()).isEqualTo(2);

        tracker.untrack(1L);
        assertThat(tracker.getTrackedCount()).isOne();
    }

    @Test
    void trackOverwritesExisting() {
        var tracker = new SessionTransportTracker();
        var t1 = new InMemoryTransport();
        var t2 = new InMemoryTransport();

        tracker.track(1L, t1);
        tracker.track(1L, t2);
        assertThat(tracker.getTransport(1L)).contains(t2);
        assertThat(tracker.getTrackedCount()).isOne();
    }

    static class InMemoryTransport implements WampTransport {
        @Override public void send(ssg.legoflow.wamp.core.WampMessage msg) {}
        @Override public ssg.legoflow.wamp.core.WampMessage receive() { return null; }
        @Override public void close() {}
        @Override public boolean isOpen() { return true; }
    }
}
