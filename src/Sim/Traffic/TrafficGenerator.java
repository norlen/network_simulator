package Sim.Traffic;

/**
 * A traffic generator is a Node that can generate traffic on a Network.
 * <p>
 * Child classes are expected to override `getNextSendTime` to provide their own delay between each packet. Each
 * received packet is forwarded to the given sink.
 */
public abstract class TrafficGenerator {
    // How many packets to send for each `StartSending` call.
    private int _packetsToSend = 0;

    // How many packets have been sent so far, since `StartSending` was called.
    private int _messagesSent = 0;

    /**
     * Creates a new `Node` that is a traffic generator.
     */
    public TrafficGenerator(int packetsToSend) {
        _packetsToSend = packetsToSend;
        _messagesSent = 0;
    }

    public boolean shouldSend() {
        return _messagesSent < _packetsToSend;
    }

    public int getMessagesSent() {
        return _messagesSent;
    }

    /**
     * Gets the time for when to send the next packet.
     *
     * @return the delay in milliseconds when the next packet should be sent.
     */
    public double getNextSendTime() {
        return 1;
    }

    public void addPacketSent() {
        _messagesSent += 1;
    }
}
