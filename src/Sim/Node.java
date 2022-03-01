package Sim;

import Sim.Events.UpdateInterface;
import Sim.Events.UpdateInterfaceAck;
import Sim.Messages.MobileIPv6.BindingUpdate;
import Sim.Traffic.Sink;
import Sim.Traffic.TrafficGenerator;

// This class implements a node (host) it has an address, a peer that it communicates with
// and it count messages send and received.
public class Node extends SimEnt {
    // The node's own address.
    protected NetworkAddr _id;

    // Link
    protected SimEnt _peer;

    //
    private final TrafficGenerator _trafficGenerator;
    private final Sink _sink;

    // Destination address to sent to.
    private NetworkAddr _dst;

    // Current sequence number for each packet.
    private int _seq = 0;

    public Node(int network, int node, TrafficGenerator generator, Sink sink) {
        super();
        _id = new NetworkAddr(network, node);
        _trafficGenerator = generator;
        _sink = sink;
    }

    // Sets the peer to communicate with. This node is single homed
    public void setPeer(SimEnt peer) {
        _peer = peer;

        if (_peer instanceof Link link) {
            link.setConnector(this);
        }
    }

    public NetworkAddr getAddr() {
        return _id;
    }


    /**
     * Start sending packets to the destination, stops when all packets have been sent.
     *
     * @param dst      Destination address to send to.
     * @param startSeq Starting sequence number.
     */
    public void StartSending(NetworkAddr dst, int startSeq) {
        _dst = dst;
        _seq = startSeq;
        send(this, new TimerEvent(), 0);
    }

    // This method is called upon that an event destined for this node triggers.
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof TimerEvent) {
            if (_trafficGenerator != null && _trafficGenerator.shouldSend()) {
                System.out.println("Node " + _id.networkId() + "." + _id.nodeId() + " sent message with seq: " + _seq + " at time " + SimEngine.getTime());

                if (_trafficGenerator.getMessagesSent() == 5) {
                    send(_peer, new UpdateInterface(_id, 3, 3), 0);
                }

                // Send message.
                var message = new Message(_id, _dst, _seq);
                send(_peer, message, 0);
                _trafficGenerator.addPacketSent();
                _seq += 1;

                // Schedule next message.
                double nextSendTime = _trafficGenerator.getNextSendTime();
                send(this, new TimerEvent(), nextSendTime);
            }
        }

        if (ev instanceof Message) {
            if (ev instanceof BindingUpdate msg) {
                System.out.println("-- Node " + _id + " receives Binding Update with new addr " + msg.getNewAddr() + ", seq: " + ((Message) ev).seq() + " at time " + SimEngine.getTime());
                _dst = msg.getNewAddr();
            } else {
                System.out.println("Node " + _id + " receives message with seq: " + ((Message) ev).seq() + " at time " + SimEngine.getTime());
                if (_sink != null) {
                    _sink.process(src, ev);
                }
            }
        }

        if (ev instanceof UpdateInterfaceAck event) {
            if (event.getChangedInterface()) {
                System.out.println("Node " + _id + " successfully changed to interface " + event.getInterfaceId() + " with network " + event.getNetworkId());
                var newNetworkAddr = new NetworkAddr(event.getNetworkId(), _id.nodeId());

                var msg = new BindingUpdate(_id, _dst, _seq, newNetworkAddr);
                _id = newNetworkAddr;

                send(_peer, msg, 0);
            } else {
                System.out.println("Node " + _id + " failed to change interface");
            }
        }
    }

    public TrafficGenerator getTrafficGenerator() {
        return _trafficGenerator;
    }

    public Sink getSink() {
        return _sink;
    }
}
