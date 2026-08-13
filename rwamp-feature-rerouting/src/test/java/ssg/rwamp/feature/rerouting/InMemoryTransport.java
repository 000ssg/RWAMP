package ssg.rwamp.feature.rerouting;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class InMemoryTransport implements WampTransport {

    private final BlockingQueue<WampMessage> sendQueue;
    private final BlockingQueue<WampMessage> receiveQueue;

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
        sendQueue.offer(msg);
    }

    @Override
    public WampMessage receive() {
        try {
            return receiveQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
    }

    WampMessage poll() {
        return receiveQueue.poll();
    }

    @Override
    public void close() {}

    @Override
    public boolean isOpen() { return true; }
}
