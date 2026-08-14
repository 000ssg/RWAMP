package ssg.rwamp.feature.statistics;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * In-memory WAMP transport for testing.
 */
class InMemoryTransport implements WampTransport {

    private final BlockingQueue<WampMessage> sendQueue;
    private final BlockingQueue<WampMessage> receiveQueue;
    private volatile boolean open = true;

    private InMemoryTransport(BlockingQueue<WampMessage> sendQueue,
                              BlockingQueue<WampMessage> receiveQueue) {
        this.sendQueue = sendQueue;
        this.receiveQueue = receiveQueue;
    }

    static InMemoryTransport[] createPair() {
        var q1 = new LinkedBlockingQueue<WampMessage>();
        var q2 = new LinkedBlockingQueue<WampMessage>();
        return new InMemoryTransport[]{
                new InMemoryTransport(q1, q2),
                new InMemoryTransport(q2, q1)
        };
    }

    @Override
    public void send(WampMessage msg) {
        if (!open) throw new IllegalStateException("Transport is closed");
        sendQueue.offer(msg);
    }

    @Override
    public WampMessage receive() {
        try {
            return receiveQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for message", e);
        }
    }

    WampMessage tryReceive() {
        return receiveQueue.poll();
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }
}
