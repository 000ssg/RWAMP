package ssg.rwamp.feature.statistics;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.transport.WampTransport;

/**
 * A {@link WampTransport} decorator that counts WAMP messages.
 * <p>
 * Wraps an existing transport and increments counters in the provided
 * {@link WampStatistics} for each message sent and received.
 * Categorizes messages by type: calls, results, errors, publishes, etc.
 * <p>
 * Usage:
 * <pre>{@code
 * var stats = new WampStatistics();
 * var transport = new StatisticsTransport(rawTransport, stats.group("myRealm"));
 * }</pre>
 *
 * @since 0.1.0
 */
public class StatisticsTransport implements WampTransport {

    private final WampTransport delegate;
    private final WampStatistics.CounterGroup counters;

    /**
     * Creates a statistics-tracking transport.
     *
     * @param delegate the underlying transport
     * @param counters the counter group to update
     */
    public StatisticsTransport(WampTransport delegate, WampStatistics.CounterGroup counters) {
        this.delegate = delegate;
        this.counters = counters;
    }

    @Override
    public void send(WampMessage msg) {
        counters.messageOut();
        countMessage(msg);
        delegate.send(msg);
    }

    @Override
    public WampMessage receive() {
        var msg = delegate.receive();
        if (msg != null) {
            counters.messageIn();
            countMessage(msg);
        }
        return msg;
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    /**
     * Returns the underlying transport.
     */
    public WampTransport getDelegate() {
        return delegate;
    }

    private void countMessage(WampMessage msg) {
        switch (msg) {
            case WampMessage.Call call -> counters.call();
            case WampMessage.Result result -> counters.result();
            case WampMessage.Error error -> counters.error();
            case WampMessage.Publish publish -> counters.publish();
            case WampMessage.Event event -> counters.event();
            case WampMessage.Subscribe subscribe -> counters.subscribe();
            case WampMessage.Register register -> counters.register();
            default -> {}
        }
    }
}
