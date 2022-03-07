package Sim;

import Sim.Events.Connected;
import Sim.Events.EnterNetwork;
import Sim.Events.LeaveNetwork;
import Sim.Messages.ICMPv6.RouterAdvertisement;
import Sim.Messages.ICMPv6.RouterSolicitation;
import Sim.Messages.MobileIPv6.BindingAck;
import Sim.Messages.MobileIPv6.BindingUpdate;
import Sim.Traffic.Sink;
import Sim.Traffic.TrafficGenerator;

// This class implements a node (host) it has an address, a peer that it communicates with
// and it count messages send and received.
public class Node extends SimEnt {
    // ------------------------------------------------------------------------
    // ADDRESS
    // ------------------------------------------------------------------------

    // The node's own address.
    protected NetworkAddr _id;

    // The node's care of address.
    protected NetworkAddr _careOfAddress;

    protected Router _router;

    void setRouter(Router router) {
        _router = router;
    }

    // ------------------------------------------------------------------------
    // LINK
    // ------------------------------------------------------------------------

    // If the node's link is connected has connected to a valid interface.
    private boolean _interfaceEnabled = false;

    // Link
    protected SimEnt _peer;

    // ------------------------------------------------------------------------
    // TRAFFIC
    // ------------------------------------------------------------------------

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
    @Override
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof Connected event) {
            processConnected(event);
        } else if (ev instanceof LeaveNetwork event) {
            processLeaveNetwork(event);
        } else if (ev instanceof TimerEvent event) {
            processTimerEvent(event);
        } else if (ev instanceof Message msg) {
            if (ev instanceof RouterAdvertisement event) {
                processRouterAdvertisement(event);
            } else if (ev instanceof BindingAck event) {
                processBindingUpdateAck(event);
            } else {
                // Generic message, no specific handling.
                System.out.println("Node " + _id + " receives message with seq: " + ((Message) ev).seq() + " at time " + SimEngine.getTime());
                if (_sink != null) {
                    _sink.process(src, ev);
                }
            }
        }
//        if (ev instanceof BindingUpdate msg) {
//            System.out.println("-- Node " + _id + " receives Binding Update with new addr " + msg.getNewAddr() + ", seq: " + ((Message) ev).seq() + " at time " + SimEngine.getTime());
//            _dst = msg.getNewAddr();
//        }
//        if (ev instanceof UpdateInterfaceAck event) {
//            if (event.getChangedInterface()) {
//                System.out.println("Node " + _id + " successfully changed to interface " + event.getInterfaceId() + " with network " + event.getNetworkId());
//                var newNetworkAddr = new NetworkAddr(event.getNetworkId(), _id.nodeId());
//
//                var msg = new BindingUpdate(_id, _dst, _seq++);
//                _id = newNetworkAddr;
//
//                send(_peer, msg, 0);
//            } else {
//                System.out.println("Node " + _id + " failed to change interface");
//            }
//        }
    }

    protected void processTimerEvent(TimerEvent event) {
        if (_trafficGenerator != null && _trafficGenerator.shouldSend()) {
            System.out.println("Node " + _id.networkId() + "." + _id.nodeId() + " sent message with seq: " + _seq + " at time " + SimEngine.getTime());

            if (_trafficGenerator.getMessagesSent() == 5) {
                var joinMsg = new EnterNetwork(this, _peer, 3, 3);
                send(_router, joinMsg, 0);
                sendMessage(joinMsg, 0);
//                sendMessage(new UpdateInterface(this, _id, 3, 3), 0);
            }

            // Send message.
            var msg = new Message(_id, _dst, _seq++);
            sendMessage(msg, 0);
            _trafficGenerator.addPacketSent();

            // Schedule next message.
            double nextSendTime = _trafficGenerator.getNextSendTime();
            send(this, new TimerEvent(), nextSendTime);
        }
    }

    protected void processLeaveNetwork(LeaveNetwork ev) {
        // We left the previous network, so make sure we don't send any traffic on the link.
        _interfaceEnabled = false;
    }

    protected void processConnected(EnterNetwork ev) {
        // We entered a network, so enable the interface again.
        _interfaceEnabled = true;

        // Send a Router Solicitation straight away, we skip the random delay since we are a mobile node.
        // RFC 4861 (https://datatracker.ietf.org/doc/html/rfc4861) mentions that the delay may be omitted for this.
        var msg = new RouterSolicitation(_seq++);
        sendMessage(msg, 0);
    }

    protected void processRouterAdvertisement(RouterAdvertisement ev) {
        var newNetwork = ev.destination().networkId();

        // If we received another advertisement for a network we're already on, then do nothing.
        if ((_careOfAddress != null && _careOfAddress.networkId() == newNetwork) || (_careOfAddress == null && _id.networkId() == newNetwork)) {
            return;
        }

        // Send a binding update to our Home Agent to notify it of our new address.
        BindingUpdate msg;
        if (newNetwork == _id.networkId()) {
            // Back on the home network.
            _careOfAddress = null;
            msg = new BindingUpdate(_id, _id, _seq++);
        } else {
            // On a foreign network.
            _careOfAddress = new NetworkAddr(newNetwork, _id.nodeId());
            msg = new BindingUpdate(_careOfAddress, _id, _seq++);
        }
        sendMessage(msg, 0);
    }

    protected void processBindingUpdateAck(BindingAck ev) {

    }

    public TrafficGenerator getTrafficGenerator() {
        return _trafficGenerator;
    }

    public Sink getSink() {
        return _sink;
    }

    protected void sendMessage(Event ev, int delayExecution) {
        if (_interfaceEnabled) {
            send(_peer, ev, delayExecution);
        }
    }
}
