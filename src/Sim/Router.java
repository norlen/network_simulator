package Sim;

import Sim.Events.EnterNetwork;
import Sim.Events.LeaveNetwork;
import Sim.Messages.ICMPv6.*;
import Sim.Messages.IPv6Tunneled;
import Sim.Messages.MobileIPv6.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * todo
 */
public class Router extends SimEnt {
    // The router's name.
    private final String _name;

    // Routing table, which holds a prefix which corresponds to an interface.
    private ArrayList<RouteTableEntry> _routingTable = new ArrayList<>();

    // Notes if the interfaces are currently in use.
    private final SimEnt[] _interfaces;

    // The router's base prefix which it uses to generate networks on its interfaces.
    private final long _basePrefix;

    // Cache for home addresses to current care of addresses.
    private HashMap<Long, NetworkAddr> _bindingCache = new HashMap<>();

    // Proxy advertisement cache. To send out Proxy Router Advertisements, we store advertisements received by
    // neighboring routers.
    private final HashMap<String, Long> _rtAdv = new HashMap<String, Long>();

    // When created, number of interfaces are defined
    public Router(String name, int interfaces, long basePrefix) {
        _interfaces = new SimEnt[interfaces];
        _basePrefix = basePrefix;
        _name = name;
    }

    // This method connects links to the router and also informs the
    // router of the host connects to the other end of the link
    public void connectInterface(int interfaceNumber, long prefix, int numBits, Link link) {
        if (interfaceNumber < _interfaces.length) {
            var entry = new RouteTableEntry(link, prefix, numBits, interfaceNumber);
            _interfaces[interfaceNumber] = link;
            _routingTable.add(entry);
            Collections.sort(_routingTable, (lhs, rhs) -> {
                if (lhs.getNetworkId() < rhs.getNetworkId()) {
                    return -1;
                } else if (lhs.getNetworkId() > rhs.getNetworkId()) {
                    return 1;
                } else {
                    return 0;
                }
            });
        } else {
            System.out.printf("ERR: %s: trying to connect to port not in router", this);
        }

        link.setConnector(this);
    }

    /**
     * Disconnect the interface having a certain network id.
     *
     * @param networkId
     */
    public void disconnectInterface(long networkId) {
        // Find all the table entries that map to this interface and remove those.
        ArrayList<RouteTableEntry> keep = new ArrayList<>();
        for (RouteTableEntry entry : _routingTable) {
            if (entry.getNetworkId() != networkId) {
                keep.add(entry);
            } else {
                _interfaces[entry.getInterfaceId()] = null;
            }
        }
        _routingTable = keep;
    }

    // This method searches for an entry in the routing table that matches
    // the network number in the destination field of a messages. The link
    // represents that network number is returned
    private SimEnt getInterface(long networkAddress) {
        for (var entry : _routingTable) {
            if (entry.matches(networkAddress)) {
                return entry.link();
            }
        }
        return null;
    }

    /**
     * Processing of received messages.
     *
     * @param src entity that sent the message.
     * @param ev  the event itself.
     */
    @Override
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof EnterNetwork event) {
            System.out.printf("== %s handle EnterNetwork%n", this);
            processEnterNetwork(src, event);
        } else if (ev instanceof LeaveNetwork event) {
            System.out.printf("== %s handles LeaveNetwork%n", this);
            processLeaveNetwork(src, event);
        } else if (ev instanceof ICMPv6 msg) {
            System.out.printf("%s handles ICMP message with seq: %d from addr: %s%n", this, msg.seq(), msg.source());
            processICMPMessage(src, msg);
        } else if (ev instanceof MobilityHeader msg) {
            System.out.printf("%s handles mobility message with seq: %d from addr: %s%n", this, msg.seq(), msg.source());
            processMobilityMessage(src, msg);
        } else if (ev instanceof IPv6Tunneled msg) {
            System.out.printf("%s handles tunneled message with seq: %d from addr: %s%n", this, msg.seq(), msg.source());
            processTunneledMessage(msg);
        } else if (ev instanceof Message msg) {
            System.out.printf("%s handles message with seq: %d from addr: %s%n", this, msg.seq(), msg.source());
            // For regular data messages, just forward those.
            forwardMessage(msg);
        }
    }

    /**
     * Handles the processing of ICMPv6 messages.
     *
     * @param src entity that sent the message.
     * @param ev  the ICMPv6 message.
     */
    protected void processICMPMessage(SimEnt src, ICMPv6 ev) {
        if (ev instanceof RtSolPr msg) {
            processRtSolPr(msg);
        } else if (ev instanceof PrRtAdv msg) {
            processPrRtAdv(msg);
        } else if (ev instanceof RouterSolicitation msg) {
            processRouterSolicitation(msg);
        } else if (ev instanceof RouterAdvertisement msg) {
            processRouterAdvertisement(msg);
        }
    }

    /**
     * Handles the processing of MobilityHeader messages.
     *
     * @param src the entity that sent the message.
     * @param ev  the Mobility Header message.
     */
    protected void processMobilityMessage(SimEnt src, MobilityHeader ev) {
        if (ev instanceof BindingUpdate msg) {
            processBindingUpdate(msg);
        } else if (ev instanceof BindingAck msg) {
            processBindingAck(msg);
        } else if (ev instanceof FastBindingUpdate msg) {
            processFastBindingUpdate(msg);
        } else if (ev instanceof FastBindingAck msg) {
            processFastBindingAck(msg);
        } else if (ev instanceof HandoverInitiate msg) {
            processHandoverInitiate(msg);
        } else if (ev instanceof HandoverAcknowledge msg) {
            processHandoverAck(msg);
        }
    }

    /**
     * Process a IPv6Tunneled message. This is separate function from the regular forwarding so Home Agents can
     * unpack and send the MN.
     *
     * @param ev the tunneled message.
     */
    protected void processTunneledMessage(IPv6Tunneled ev) {
        // If we received a tunneled message where the source is a mobile agent we care for, then we should unwrap it
        // and forward the original message.
        boolean caresFor = false;
        for (var entry : _bindingCache.entrySet()) {
            if (entry.getValue() == ev.source()) {
                caresFor = true;
                break;
            }
        }

        if (caresFor) {
            var original = ev.getOriginalPacket();
            System.out.printf("%s received tunneled message %d from %s which it cares for. Unpacking and forwarding to %s", this, ev.seq(), ev.source(), original.destination());
            forwardMessage(original);
        } else {
            System.out.printf("%s received tunneled message %d. Do nothing and pass along to %s", this, ev.seq(), ev.destination());
            forwardMessage(ev);
        }
    }

    /**
     * Handle event when a node joined the network and wants to bind to an interface.
     *
     * @param src link of joined node.
     * @param ev  the join event.
     */
    protected void processEnterNetwork(SimEnt src, EnterNetwork ev) {
        var interfaceId = ev.getInterfaceId();
        if (_interfaces[interfaceId] != null) {
            // Cannot bind to an interface that's already in use.
            System.err.printf("ERR: %s: cannot bind to interface %d, already in use%n", this, interfaceId);
            return;
        }

        connectInterface(interfaceId, _basePrefix + interfaceId, 64, (Link) src);
    }

    /**
     * Handle event for when a node leaves the network and the interface should be released.
     *
     * @param src link that left the network.
     * @param ev  leave network event.
     */
    protected void processLeaveNetwork(SimEnt src, LeaveNetwork ev) {
        disconnectInterface(ev.getSourceAddress().networkId());
    }

    /**
     * Handle Router Solicitation messages, they essentially request a Router Advertisement to be sent.
     *
     * @param ev Solicitation message.
     */
    protected void processRouterSolicitation(RouterSolicitation ev) {
        for (int i = 0; i < _interfaces.length; ++i) {
            var link = _interfaces[i];
            if (link == null) continue;

            // Generate a new network for something that connected.
            long network = _basePrefix + i;

            // Advertise the network prefix as the current interface id. We don't have an address for routers, so use
            // the unspecified address for now.
            var msg = new RouterAdvertisement(NetworkAddr.UNSPECIFIED, NetworkAddr.ALL_NODES_MULTICAST, 0, _name, network);
            send(link, msg, 0);
        }
    }

    /**
     * Process a Router Advertisement message. For regular routers, we basically do nothing, we don't propagate these
     * over the network.
     *
     * @param ev Router Advertisement message.
     */
    protected void processRouterAdvertisement(RouterAdvertisement ev) {
        // To support the Fast Mobile IPv6 Handovers we store these advertisements, so we can respond to RtSolPr
        // requests, where a Mobile Node can solicit advertisements from nearby routers.
        String apName = ev.getName();
        long apPrefix = ev.getNetworkPrefix();
        _rtAdv.put(apName, apPrefix);
    }

    /**
     * Handle the Router Solicitation for Proxy Advertisement messages.
     * <p>
     * So here we have a node on our network that wants advertisements for a router on a neighboring network. So we send
     * back advertisements we have in our cache.
     *
     * @param ev the Router Solicitation for Proxy Advertisement message.
     */
    protected void processRtSolPr(RtSolPr ev) {
        String narName = ev.getNextAccessRouterName();
        if (_rtAdv.containsKey(narName)) {
            long networkPrefix = _rtAdv.get(narName);
            var advertisement = new PrRtAdv(NetworkAddr.UNSPECIFIED, ev.source(), 0, narName, networkPrefix);
            forwardMessage(advertisement);
        }
    }

    /**
     * Handle the Proxy Router Advertisements.
     * <p>
     * For routers if we receive this, as these are probably multicasted, we basically do not propagate these over the
     * network.
     *
     * @param ev the Proxy Router Advertisement message.
     */
    protected void processPrRtAdv(PrRtAdv ev) {
        // Do nothing.
    }

    /**
     * Handle the Binding Update message. The router acts as a Home Agent, so if it receives this for an address it
     * manages, it will update the binding cache with the new care of address.
     *
     * @param ev the Binding Update message.
     */
    protected void processBindingUpdate(BindingUpdate ev) {
        // Check if this binding update is intended for this router.
        if (ev.destination().networkId() == _basePrefix) {
            // This should be forwarded to the correct HA.
            forwardMessage(ev);
        } else {
            // This is intended for us, so add a care of address.
            var homeAddress = ev.destination();
            var careOfAddress = ev.source();

            System.out.printf("== %s update binding from %s to %s%n", this, homeAddress, careOfAddress);
            _bindingCache.put(homeAddress.networkId(), careOfAddress);
        }
    }

    /**
     * Handle the Binding Ack message. Routers should not receive this, so we just drop them.
     *
     * @param ev Binding Ack message.
     */
    protected void processBindingAck(BindingAck ev) {
        // Do nothing.
    }

    /**
     * Handle the Fast Binding Update messages. These are part of the Fast Mobile IPv6 Handovers, but the message is
     * basically the same as the regular Binding Update, the difference lies in how they are handled.
     *
     * @param ev
     */
    protected void processFastBindingUpdate(FastBindingUpdate ev) {
        // todo
    }

    /**
     * Handle the Fast Binding Ack message. These are similar to the regular Binding Ack messages, but routers can
     * receive these and must handle them.
     *
     * @param ev the Fast Binding Ack message.
     */
    protected void processFastBindingAck(FastBindingAck ev) {
        // todo
    }

    /**
     * Handle the Handover Initiate message.
     * <p>
     * These are sent from the current access router to the next access router to initiate a handover for the MN.
     *
     * @param ev
     */
    protected void processHandoverInitiate(HandoverInitiate ev) {
        // todo
    }

    /**
     * Handle the Handover Acknowledge message.
     * <p>
     * These are sent from the next access router to the current in response to a HandoverInitiate message.
     *
     * @param ev the Handover Acknowledge message
     */
    protected void processHandoverAck(HandoverAcknowledge ev) {
        // todo
    }

    /**
     * Finds the interface based on the destination address and forwards the message to the correct interface.
     *
     * @param ev message to forward.
     */
    protected void forwardMessage(Message ev) {
        if (_bindingCache.containsKey(ev.destination().networkId())) {
            var coa = _bindingCache.get(ev.destination().networkId());
            System.out.printf("== %s tunnels message from (%s -> %s) to (%s -> %s)%n", this, ev.source(), ev.destination(), ev.destination(), coa);
            ev = new IPv6Tunneled(ev.destination(), coa, 0, ev);
        }

        SimEnt sendNext = getInterface(ev.destination().networkId());
        if (sendNext == null) {
            System.out.printf("ERR: %s wants to send to %s but interface is unbound%n", this, ev.destination());
        } else {
            System.out.printf("%s sends to %s%n", this, ev.destination());
            send(sendNext, ev, 0);
        }
    }

    /**
     * Returns a string that identifies the router using its name.
     *
     * @return the string "Router <name>".
     */
    @Override
    public String toString() {
        return String.format("Router %s", _name);
    }

    /**
     * Pretty prints the routing tables to standard out.
     */
    protected void debugRoutingTables() {
        System.out.printf("%s Routing table:%n", this);
        for (var entry : _routingTable) {
            System.out.printf("  networkId: %h, bits: %d%n -> interface: %d%n", entry.getNetworkId(), entry.getNumBits(), entry.getInterfaceId());
        }
    }
}
