package Sim;

import Sim.Events.*;
import Sim.Messages.ICMPv6.*;
import Sim.Messages.IPv6Tunneled;
import Sim.Messages.MobileIPv6.*;
import Sim.Traffic.CountingSink;
import Sim.Traffic.Sink;
import Sim.Traffic.TrafficGenerator;

/**
 * A Mobile node that supports fast handovers.
 */
public class Node extends SimEnt {
    // Name for the node.
    private final String _name;

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

    // Fields that are required for us to store when performing a fast handover.
    private record Handover(NetworkAddr newCareOfAddress, SimEnt router, int interfaceId) {
    }

    // Keep track of the state when performing a fast handover.
    private Handover _handover = null;

    // Statistics for received packets.
    private int _pktsReceived = 0;
    private int _tunneledPktsReceived = 0;
    private EnterNetwork _connectNext = null;

    public Node(String name, NetworkAddr addr, NetworkAddr haAddress, TrafficGenerator generator, Sink sink) {
        super();
        _name = name;
        _linkLocal = new NetworkAddr(0xfe80000000000000L, addr.nodeId());
        _homeAddress = addr;
        _homeAgent = haAddress;
        _trafficGenerator = generator;
        _sink = sink;
    }

    /**
     * Sets the peer to communicate with. This node is single homed
     *
     * @param peer link to connect to.
     */
    public void setPeer(SimEnt peer) {
        _peer = peer;

        if (_peer instanceof Link link) {
            link.setConnector(this);
        }
    }

    public NetworkAddr getHomeAddress() {
        return _homeAddress;
    }

    /**
     * Gets the node's current address, which is the CoA if it is configured, otherwise it is the home address.
     *
     * @return node's current address.
     */
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
        }
    }

    /**
     * Handle ICMPv6 messages.
     *
     * @param ev ICMPv6 Message.
     */
    public void processICMPMessage(ICMPv6 ev) {
        // Unhandled packets:
        // - RtSolPr
        // - RouterSolicitation
        if (ev instanceof PrRtAdv msg) {
            processPrRtAdv(msg);
        } else if (ev instanceof RouterAdvertisement msg) {
            processRouterAdvertisement(msg);
        }
    }

    /**
     * Process messages with a Mobility Header.
     *
     * @param ev message with mobility header.
     */
    public void processMobilityHeader(MobilityHeader ev) {
        // Unhandled packets:
        // - FastBindingUpdate
        // - BindingUpdate
        // - HandoverInitiate
        // - HandoverAcknowledge.
        if (ev instanceof FastBindingAck msg) {
            processFastBindingAck(msg);
        } else if (ev instanceof BindingAck msg) {
            processBindingUpdateAck(msg);
        }
    }

    /**
     * Process generic messages. This includes tunneled messages.
     *
     * @param src entity we got message from.
     * @param ev  generic message.
     */
    public void processMessage(SimEnt src, Message ev) {
        if (ev instanceof IPv6Tunneled msg) {
            System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);
            recv(src, msg.getOriginalPacket());
            _tunneledPktsReceived += 1;
            return;
        }

        // Generic message, no specific handling.
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);
        _pktsReceived += 1;
        if (_sink != null) {
            _sink.process(src, ev);
        }
    }

    /**
     * Handle the connected event. This is received after the link has established a connection to a network.
     *
     * @param ev connected event.
     */
    protected void processConnected(Connected ev) {
        System.out.printf("[%d] %s: [%s] connected to new network%n", (int) SimEngine.getTime(), this, ev);

        // Only send if we have not configured our IPs yet, we might have done this if we performed a fast handover.
        if (_homeAddress == null || _careOfAddress == null) {
            // Send a Router Solicitation straight away, we skip the random delay since we are a mobile node.
            // RFC 4861 (https://datatracker.ietf.org/doc/html/rfc4861) mentions that the delay may be omitted for this.
            var msg = new RouterSolicitation(NetworkAddr.UNSPECIFIED, NetworkAddr.ALL_ROUTER_MULTICAST, _seq++);
            sendMessage(msg);
        }

        // Next sink counter.
        if (_sink instanceof CountingSink sink) {
            sink.newCount("Connected");
        }
    }

    /**
     * Handle the disconnected event. This is received after the link has disconnected from a router.
     *
     * @param ev the disconnected event.
     */
    protected void processDisconnected(Disconnected ev) {
        System.out.printf("[%d] %s: [%s] disconnected from current network%n", (int) SimEngine.getTime(), this, ev);
        if (_connectNext != null) {
            sendMessage(_connectNext);
            _connectNext = null;
        } else {
            System.out.printf("Err: %s has no router to connect to next%n", this);
        }

        // Next sink counter.
        if (_sink instanceof CountingSink sink) {
            sink.newCount("Disconnected");
        }
    }

    /**
     * Handle the TimerEvent, this is used to send messages.
     *
     * @param ignoredEv timer event.
     */
    protected void processTimerEvent(TimerEvent ignoredEv) {
        if (_trafficGenerator != null && _trafficGenerator.shouldSend()) {
            // Send message.
            var msg = new Message(_homeAddress, _dst, _seq++);
            if (_careOfAddress != null) {
                //System.out.printf("%s has CoA, tunneling message with seq: %d to %s%n", this, msg.seq(), _homeAddress);
                // If we have a care of address, tunnel the message to the home agent.
                var haAddress = new NetworkAddr(_homeAddress.networkId(), 0);
                msg = new IPv6Tunneled(_careOfAddress, haAddress, 0, msg);
            }
            sendMessage(msg);
            _trafficGenerator.addPacketSent();

            System.out.printf("[%d] %s: send [%s]%n", (int) SimEngine.getTime(), this, msg);

            // Schedule next message.
            double nextSendTime = _trafficGenerator.getNextSendTime();
            send(this, new TimerEvent(), nextSendTime);
        }
    }

    protected void processStartHandover(StartHandover ev) {
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);
        if (ev.isFastHandover()) {
            // We want to switch to a new network soon. So start the handover process.
            var msg = new RtSolPr(getCurrentAddress(), NetworkAddr.ALL_ROUTER_MULTICAST, _seq++, ev.getNextAccessRouter(), ev.getNextInterfaceId());
            sendMessage(msg);

            _handover = new Handover(null, ev.getRouter(), ev.getNextInterfaceId());
        } else {
            // Regular handover.
            sendMessage(new LeaveNetwork(getCurrentAddress()));
            _connectNext = new EnterNetwork(this, ev.getRouter(), ev.getNextInterfaceId());
            _ipConfigurationCompleted = false;
            _careOfAddress = null;
        }
    }

    protected void processRouterAdvertisement(RouterAdvertisement ev) {
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);

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
                msg = new BindingUpdate(_homeAddress, _homeAgent, _seq++, 0, _homeAddress);
            } else {
                _careOfAddress = new NetworkAddr(prefix, _linkLocal.nodeId());
                msg = new BindingUpdate(_careOfAddress, _homeAgent, _seq++, 0, _homeAddress);
            }
            sendMessage(msg);
        }
        System.out.printf("%s completed IPv6 stateless auto configuration%n", this);

        // IP configuration done. Here we should do neighbor discovery to see if this address exists on the network.
        // which is left for future work.
        _ipConfigurationCompleted = true;
    }

    protected void processPrRtAdv(PrRtAdv ev) {
        if (ev.destination() == NetworkAddr.ALL_ROUTER_MULTICAST) {
            // Skip advertisements intended for routers.
            return;
        }
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);

        var nextCareOfAddress = new NetworkAddr(ev.getNetworkPrefix(), _linkLocal.nodeId());
        var msg = new FastBindingUpdate(getCurrentAddress(), _homeAddress, _seq++, 0, _homeAddress, ev.getInterfaceName(), nextCareOfAddress);
        sendMessage(msg);

        _handover = new Handover(nextCareOfAddress, _handover.router, _handover.interfaceId);
    }

    protected void processBindingUpdateAck(BindingAck ev) {
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);
    }

    protected void processFastBindingAck(FastBindingAck ev) {
        // When we this ack, the fast handover process is completed, and we should disconnect from the current network,
        // and join the new network.
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);

        var LeaveEvent = new LeaveNetwork(getCurrentAddress());
        _connectNext = new EnterNetwork(this, _handover.router, _handover.interfaceId);
        _careOfAddress = _handover.newCareOfAddress;

        System.out.printf("%s fast handover setup complete%n", this);
        sendMessage(LeaveEvent);
    }

    /**
     * Helper to send a message to the node's link with no delay.
     *
     * @param ev message to send.
     */
    protected void sendMessage(Event ev) {
        send(_peer, ev, 0);
    }

    /**
     * Handler for when the simulation has finished. Prints the statistics the node has gathered.
     */
    public void onSimulationComplete() {
        System.out.printf("%s%n", this);
        System.out.printf("- Packets received: %d%n", _pktsReceived);
        System.out.printf("- Tunneled packets received: %d%n", _tunneledPktsReceived);
        if (_sink != null && _sink instanceof CountingSink sink) {
            sink.printCounts();
        }
    }

    @Override
    public String toString() {
        return String.format("Node %s [ha=%s, coa=%s]", _name, _homeAddress, _careOfAddress);
    }
}
