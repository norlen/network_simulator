package Sim.Traffic;

import Sim.*;

/**
 * A traffic generator is a Node that can generate traffic on a Network.
 * <p>
 * Child classes are expected to override `getNextSendTime` to provide their own delay between each packet. Each
 * received packet is forwarded to the given sink.
 */
public class TrafficGenerator extends Node {
    // How many packets to send for each `StartSending` call.
    private int _packetsToSend = 0;

    // How many packets have been sent so far, since `StartSending` was called.
    private int _messagesSent = 0;

    // Destination address to sent to.
    private NetworkAddr _dst;

    // Current sequence number for each packet.
    private int _seq = 0;

    /**
     * Creates a new `Node` that is a traffic generator.
     *
     * @param network This node's network id.
     * @param node    This node's node id.
     */
    public TrafficGenerator(int network, int node) {
        super(network, node);
    }

    /**
     * Start sending packets to the destination, stops when all packets have been sent.
     *
     * @param dst           Destination address to send to.
     * @param packetsToSend How many packets to send.
     * @param startSeq      Starting sequence number.
     */
    public void StartSending(NetworkAddr dst, int packetsToSend, int startSeq) {
        _packetsToSend = packetsToSend;
        _messagesSent = 0;
        _dst = dst;
        _seq = startSeq;
        send(this, new TimerEvent(), 0);
    }

    /**
     * Handle incoming events. Processes incoming messages and sends outgoing messages on a timer.
     *
     * @param src SimEnt entity that sent the event.
     * @param ev  Event to be processed.
     */
    @Override
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof TimerEvent) {
            if (_packetsToSend > _messagesSent) {
                _messagesSent += 1;

                var message = new Message(_id, _dst, _seq);
                _seq += 1;
                send(_peer, message, 0);

                double nextSendTime = getNextSendTime();
                send(this, new TimerEvent(), nextSendTime);
                System.out.println("Node " + _id.networkId() + "." + _id.nodeId() + " sent message with seq: " + _seq + " at time " + SimEngine.getTime());
            }
        }
        if (ev instanceof Message) {
            System.out.println("Node " + _id.networkId() + "." + _id.nodeId() + " receives message with seq: " + ((Message) ev).seq() + " at time " + SimEngine.getTime());
        }
    }

    /**
     * Gets the time for when to send the next packet.
     *
     * @return the delay in milliseconds when the next packet should be sent.
     */
    protected double getNextSendTime() {
        return 1;
    }
}
