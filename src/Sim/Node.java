package Sim;

import Sim.Events.Connected;
import Sim.Events.Disconnected;
import Sim.Events.EnterNetwork;
import Sim.Events.LeaveNetwork;
import Sim.Messages.ICMPv6.RouterAdvertisement;
import Sim.Messages.ICMPv6.RouterSolicitation;
import Sim.Messages.IPv6Tunneled;
import Sim.Messages.MobileIPv6.BindingAck;
import Sim.Messages.MobileIPv6.BindingUpdate;
import Sim.Traffic.Sink;
import Sim.Traffic.TrafficGenerator;

// This class implements a node (host) it has an address, a peer that it communicates with
// and it count messages send and received.

/**
 *
 */
public class Node extends SimEnt {
    // Link-local address.
    protected NetworkAddr _linkLocal;

    // The node's global unicast address which it got by the home agent.
    protected NetworkAddr _homeAddress;

    // The node's care of address.
    protected NetworkAddr _careOfAddress;

    // Flag to check if we have performed the stateless autoconfiguration.
    protected boolean _ipConfigurationCompleted = true;

    // Link
    protected SimEnt _peer;

    //
    private final TrafficGenerator _trafficGenerator;
    private final Sink _sink;

    // Destination address to sent to.
    private NetworkAddr _dst;

    // Current sequence number for each packet.
    private int _seq = 0;

    private final String _name;

    public Node(String name, NetworkAddr addr, TrafficGenerator generator, Sink sink) {
        super();
        _name = name;
        _linkLocal = new NetworkAddr(0xfe80000000000000L, addr.nodeId());
        _homeAddress = addr;
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

    public NetworkAddr getLinkLocalAddr() {
        return _linkLocal;
    }

    public NetworkAddr getHomeAddress() {
        return _homeAddress;
    }

    public NetworkAddr getCurrentAddress() {
        if (_careOfAddress != null) {
            return _careOfAddress;
        } else {
            return _homeAddress;
        }
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
        } else if (ev instanceof Disconnected event) {
            processDisconnected(event);
        } else if (ev instanceof TimerEvent event) {
            processTimerEvent(event);
        } else if (ev instanceof Message msg) {
            if (ev instanceof RouterAdvertisement event) {
                processRouterAdvertisement(event);
            } else if (ev instanceof BindingAck event) {
                processBindingUpdateAck(event);
            } else if (ev instanceof IPv6Tunneled event) {
                System.out.printf("-- %s receives tunneled message with seq: %d at time: %f%n", this, event.seq(), SimEngine.getTime());
                recv(src, event.getOriginalPacket());
            } else {
                // Generic message, no specific handling.
                System.out.printf("%s receives message with seq: %d at time: %f%n", this, msg.seq(), SimEngine.getTime());
                if (_sink != null) {
                    _sink.process(src, ev);
                }
            }
        }
    }

    protected void processTimerEvent(TimerEvent event) {
        if (_trafficGenerator != null && _trafficGenerator.shouldSend()) {
            System.out.printf("%s sent message to %s with seq: %d at time: %f%n", this, _dst, _seq, SimEngine.getTime());

            if (_trafficGenerator.getMessagesSent() == 5 && _name == "MN") {
                // Leave the current network and join the new network.
                System.out.printf("-- %s leaving current network and tries to join new network%n", this);
                sendMessage(new LeaveNetwork(getCurrentAddress()));
                sendMessage(new EnterNetwork(this, 3));
                _ipConfigurationCompleted = false;
                _careOfAddress = null;
            }

            // Send message.
            var msg = new Message(_homeAddress, _dst, _seq++);
            if (_careOfAddress != null) {
                System.out.printf("%s has CoA, tunneling message with seq: %d to %s%n", this, msg.seq(), _homeAddress);
                // If we have a care of address, tunnel the message to the home agent.
                msg = new IPv6Tunneled(_careOfAddress, _homeAddress, 0, msg);
            }
            sendMessage(msg);
            _trafficGenerator.addPacketSent();

            // Schedule next message.
            double nextSendTime = _trafficGenerator.getNextSendTime();
            send(this, new TimerEvent(), nextSendTime);
        }
    }

    protected void processConnected(Connected ev) {
        System.out.printf("-- %s connected to network at time: %f%n", this, SimEngine.getTime());

        // Send a Router Solicitation straight away, we skip the random delay since we are a mobile node.
        // RFC 4861 (https://datatracker.ietf.org/doc/html/rfc4861) mentions that the delay may be omitted for this.
        var msg = new RouterSolicitation(_linkLocal, _seq++);
        sendMessage(msg);
    }

    protected void processDisconnected(Disconnected ev) {
        // Remove care of address if it exists, and make sure we perform ip configuration on next connect.
//        _ipConfigurationCompleted = false;
//        _careOfAddress = null;
    }

    protected void processRouterAdvertisement(RouterAdvertisement ev) {
        System.out.printf("-- %s receives RouterAdvertisement with seq: %d at time %f%n", this, ev.seq(), SimEngine.getTime());

        if (_ipConfigurationCompleted) {
            // This isn't exactly correct, but we don't care about advertisement right now if we have already performed
            // stateless autoconfiguration.
            return;
        }

        // Check if we entered our home network or a foreign network.
        if (_homeAddress == null) {
            _homeAddress = new NetworkAddr(ev.getNetworkPrefix(), _linkLocal.nodeId());
        } else {
            var prefix = ev.getNetworkPrefix();
            BindingUpdate msg;

            // Check if we re-entered the home network.
            if (_homeAddress.networkId() == prefix) {
                msg = new BindingUpdate(_homeAddress, _homeAddress, _seq++);
            } else {
                _careOfAddress = new NetworkAddr(prefix, _linkLocal.nodeId());
                msg = new BindingUpdate(_careOfAddress, _homeAddress, _seq++);
            }
            sendMessage(msg);
        }
        System.out.printf("-- %s completed IPv6 stateless auto configuration%n", this, _homeAddress, _careOfAddress);

        // IP configuration done. Here we should do neighbor discovery to see if this address exists on the network.
        // which is left for future work.
        _ipConfigurationCompleted = true;
    }

    protected void processBindingUpdateAck(BindingAck ev) {
        // Not sure what to do here.
        System.out.printf("-- %s receives BindingAck with seq: %d at time %f%n", this, ev.seq(), SimEngine.getTime());
    }

    public TrafficGenerator getTrafficGenerator() {
        return _trafficGenerator;
    }

    public Sink getSink() {
        return _sink;
    }

    protected void sendMessage(Event ev) {
        send(_peer, ev, 0);
    }

    @Override
    public String toString() {
        return String.format("Node %s ll=[%s] ha=[%s] coa=[%s]", _name, _linkLocal, _homeAddress, _careOfAddress);
    }
}
