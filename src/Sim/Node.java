package Sim;

import Sim.Events.*;
import Sim.Messages.ICMPv6.*;
import Sim.Messages.IPv6Tunneled;
import Sim.Messages.MobileIPv6.*;
import Sim.Traffic.Sink;
import Sim.Traffic.TrafficGenerator;

// This class implements a node (host) it has an address, a peer that it communicates with
// and it count messages send and received.

// TODO: add home agent address.

/**
 *
 */
public class Node extends SimEnt {
    private record Handover(NetworkAddr newCareOfAddress, SimEnt router, int interfaceId) {
    }

    // Link-local address.
    protected NetworkAddr _linkLocal;

    // The node's global unicast address which it got by the home agent.
    protected NetworkAddr _homeAddress;

    // The node's care of address.
    protected NetworkAddr _careOfAddress;

    // Address to home agent.
    protected NetworkAddr _homeAgent;

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
    private Handover _handover = null;

    public Node(String name, NetworkAddr addr, NetworkAddr haAddress, TrafficGenerator generator, Sink sink) {
        super();
        _name = name;
        _linkLocal = new NetworkAddr(0xfe80000000000000L, addr.nodeId());
        _homeAddress = addr;
        _homeAgent = haAddress;
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
        } else if (ev instanceof StartHandover event) {
            processStartHandover(event);
        } else if (ev instanceof TimerEvent event) {
            processTimerEvent(event);
        } else if (ev instanceof ICMPv6 msg) {
            processICMPMessage(msg);
        } else if (ev instanceof MobilityHeader msg) {
            processMobilityHeader(msg);
        } else if (ev instanceof Message msg) {
            processMessage(src, msg);
        } else if (ev instanceof Message msg) {
            if (ev instanceof RouterAdvertisement event) {
                processRouterAdvertisement(event);
            } else if (ev instanceof BindingAck event) {
                processBindingUpdateAck(event);
            } else if (ev instanceof IPv6Tunneled event) {

            } else {
            }
        }
    }

    public void processICMPMessage(ICMPv6 ev) {
        if (ev instanceof PrRtAdv msg) {

        } else if (ev instanceof RtSolPr msg) {
            // Should not get.
        } else if (ev instanceof RouterAdvertisement msg) {

        } else if (ev instanceof RouterSolicitation msg) {
            // Should not get.
        }
    }

    public void processMobilityHeader(MobilityHeader ev) {
        if (ev instanceof FastBindingUpdate msg) {
            // Should not get.
        } else if (ev instanceof FastBindingAck msg) {

        } else if (ev instanceof BindingUpdate msg) {
            // Should not get.
        } else if (ev instanceof BindingAck msg) {

        } else if (ev instanceof HandoverInitiate msg) {
            // Should not get.
        } else if (ev instanceof HandoverAcknowledge msg) {
            // Should not get.
        }
    }

    public void processMessage(SimEnt src, Message ev) {
        if (ev instanceof IPv6Tunneled msg) {
            System.out.printf("-- %s receives tunneled message with seq: %d at time: %f%n", this, msg.seq(), SimEngine.getTime());
            recv(src, msg.getOriginalPacket());
            return;
        }

        // Generic message, no specific handling.
        System.out.printf("%s receives message with seq: %d at time: %f%n", this, ev.seq(), SimEngine.getTime());
        if (_sink != null) {
            _sink.process(src, ev);
        }
    }

    protected void processConnected(Connected ev) {
        System.out.printf("-- %s connected to network at time: %f%n", this, SimEngine.getTime());

        // Only send if we have not configured our IPs yet, we might have done this if we performed a fast handover.
        if (_homeAddress == null || _careOfAddress == null) {
            // Send a Router Solicitation straight away, we skip the random delay since we are a mobile node.
            // RFC 4861 (https://datatracker.ietf.org/doc/html/rfc4861) mentions that the delay may be omitted for this.
            var msg = new RouterSolicitation(NetworkAddr.UNSPECIFIED, NetworkAddr.ALL_ROUTER_MULTICAST, _seq++);
            sendMessage(msg);
        }
    }

    protected void processDisconnected(Disconnected ev) {
    }

    protected void processTimerEvent(TimerEvent event) {
        if (_trafficGenerator != null && _trafficGenerator.shouldSend()) {
            System.out.printf("%s sent message to %s with seq: %d at time: %f%n", this, _dst, _seq, SimEngine.getTime());

            if (_trafficGenerator.getMessagesSent() == 5 && _name == "MN") {
                // Leave the current network and join the new network.
                System.out.printf("-- %s leaving current network and tries to join new network%n", this);
                sendMessage(new LeaveNetwork(getCurrentAddress()));
                sendMessage(new EnterNetwork(this, null, 3));
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

    protected void processStartHandover(StartHandover ev) {
        // We want to switch to a new network soon. So start the handover process.
        var msg = new RtSolPr(getCurrentAddress(), NetworkAddr.ALL_ROUTER_MULTICAST, _seq++, ev.getNextAccessRouter(), ev.getNextInterfaceId());
        sendMessage(msg);

        _handover = new Handover(null, ev.getRouter(), ev.getNextInterfaceId());
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
                msg = new BindingUpdate(_homeAddress, _homeAgent, _seq++);
            } else {
                _careOfAddress = new NetworkAddr(prefix, _linkLocal.nodeId());
                msg = new BindingUpdate(_careOfAddress, _homeAgent, _seq++);
            }
            sendMessage(msg);
        }
        System.out.printf("-- %s completed IPv6 stateless auto configuration%n", this, _homeAddress, _careOfAddress);

        // IP configuration done. Here we should do neighbor discovery to see if this address exists on the network.
        // which is left for future work.
        _ipConfigurationCompleted = true;
    }

    protected void processPrRtAdv(PrRtAdv ev) {
        var nextCareOfAddress = new NetworkAddr(ev.getNetworkPrefix(), _linkLocal.nodeId());
        var msg = new FastBindingUpdate(getCurrentAddress(), _homeAddress, _seq++, ev.getInterfaceName(), nextCareOfAddress);
        sendMessage(msg);

        _handover = new Handover(nextCareOfAddress, _handover.router, _handover.interfaceId);
    }

    protected void processBindingUpdateAck(BindingAck ev) {
        System.out.printf("-- %s receives BindingAck with seq: %d at time %f%n", this, ev.seq(), SimEngine.getTime());
    }

    protected void processFastBindingAck(FastBindingAck ev) {
        // When we this ack, the fast handover process is completed, and we should disconnect from the current network,
        // and join the new network.
        System.out.printf("-- %s receives FastBindingAck with seq: %d at time %f%n", this, ev.seq(), SimEngine.getTime());

        var LeaveEvent = new LeaveNetwork(getCurrentAddress());
        var JoinEvent = new EnterNetwork(this, _handover.router, _handover.interfaceId);
        _careOfAddress = _handover.newCareOfAddress;

        sendMessage(LeaveEvent);
        sendMessage(JoinEvent);
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
