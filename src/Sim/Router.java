package Sim;

import Sim.Events.EnterNetwork;
import Sim.Events.LeaveNetwork;
import Sim.Messages.ICMPv6.*;
import Sim.Messages.IPv6Tunneled;
import Sim.Messages.MobileIPv6.*;

import java.util.ArrayList;
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

    // Base address to generate network addresses from.
    private final NetworkAddr _baseAddress;

    // Cache for home addresses to current care of addresses.
    private final HashMap<NetworkAddr, NetworkAddr> _bindingCache = new HashMap<>();

    // Hard-coded advertisements from neighbor routers, used for proxy advertisements.
    //private final HashMap<String, PrRtAdv> _proxyAdvertisements = new HashMap<>();

    // So to make this work we send proxy advertisements to other routers, so they can store those for routers that are
    // one hop away.

    private int _timeBetweenAdvertisements;

    private record ProxyAdvertisementEntry(NetworkAddr interfaceAddr,
                                           NetworkAddr from,
                                           PrRtAdv advertisement) {
    }

    private final HashMap<String, ProxyAdvertisementEntry> _proxyAdvertisements = new HashMap<>();

    private record FastHandover(int sequence,
                                NetworkAddr homeAgentAddress,
                                NetworkAddr homeAddress,
                                NetworkAddr currentCareOfAddress,
                                NetworkAddr nextCareOfAddress) {
    }

    private final HashMap<Integer, FastHandover> _handovers = new HashMap<>();

    // Count of how many packets we dropped, because no interface could be found for the address.
    private int _pktsDroppedNoInterface = 0;

    /**
     * Instantiate a new router.
     *
     * @param name       name of the router.
     * @param interfaces maximum number of interfaces.
     * @param baseAddr   address to use as base for network addresses.
     */
    public Router(String name, int interfaces, NetworkAddr baseAddr) {
        _name = name;
        _interfaces = new SimEnt[interfaces];
        _baseAddress = baseAddr;
    }

    /**
     * Make the router send out proxy advertisements on a delay, and not just when it gets a solicitation. Set delay to
     * zero to only send once.
     *
     * @param delay time between advertisements.
     */
    public void startSendingProxyAdvertisements(int delay) {
        _timeBetweenAdvertisements = delay;
        send(this, new TimerEvent(), 0);
    }

    // This method connects links to the router and also informs the
    // router of the host connects to the other end of the link
    public void connectInterface(int interfaceNumber, NetworkAddr addr, Link link) {
        if (interfaceNumber < _interfaces.length) {
            var entry = new RouteTableEntry(link, addr, interfaceNumber);
            _interfaces[interfaceNumber] = link;
            _routingTable.add(entry);
            _routingTable.sort((lhs, rhs) -> rhs.getAddr().getPrefixBits() - lhs.getAddr().getPrefixBits());
            debugRoutingTables();
        } else {
            System.out.printf("ERR: %s: trying to connect to port not in router", this);
        }

        link.setConnector(this);
    }

    /**
     * Disconnect the interface having a certain network id.
     *
     * @param networkId which network to disconnect.
     */
    public void disconnectInterface(long networkId) {
        // Find all the table entries that map to this interface and remove those.
        ArrayList<RouteTableEntry> keep = new ArrayList<>();
        for (RouteTableEntry entry : _routingTable) {
            if (entry.getAddr().networkId() != networkId) {
                keep.add(entry);
            } else {
                var link = (Link) _interfaces[entry.getInterfaceId()];
                if (link != null) {
                    _interfaces[entry.getInterfaceId()] = null;
                    link.setConnector(null);
                }
            }
        }
        _routingTable = keep;
    }

    // This method searches for an entry in the routing table that matches
    // the network number in the destination field of a messages. The link
    // represents that network number is returned
    private SimEnt getInterface(long networkAddress) {
        for (var entry : _routingTable) {
            if (entry.getAddr().matches(networkAddress)) {
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
            processEnterNetwork(src, event);
        } else if (ev instanceof LeaveNetwork event) {
            processLeaveNetwork(src, event);
        } else if (ev instanceof TimerEvent) {
            System.out.printf("[%d] %s: send Proxy Advertisement to other routers%n", (int) SimEngine.getTime(), this);
            sendProxyAdvertisements();
            if (_timeBetweenAdvertisements != 0) {
                send(this, new TimerEvent(), _timeBetweenAdvertisements);
            }
        } else if (ev instanceof ICMPv6 msg) {
            processICMPMessage(src, msg);
        } else if (ev instanceof MobilityHeader msg) {
            processMobilityMessage(src, msg);
        } else if (ev instanceof IPv6Tunneled msg) {
            processTunneledMessage(msg);
        } else if (ev instanceof Message msg) {
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
            processRtSolPr(src, msg);
        } else if (ev instanceof PrRtAdv msg) {
            processPrRtAdv(src, msg);
        } else if (ev instanceof RouterSolicitation msg) {
            processRouterSolicitation(src, msg);
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
        if (ev instanceof FastBindingUpdate msg) {
            processFastBindingUpdate(msg);
        } else if (ev instanceof FastBindingAck msg) {
            processFastBindingAck(msg);
        } else if (ev instanceof BindingUpdate msg) {
            processBindingUpdate(msg);
        } else if (ev instanceof BindingAck msg) {
            processBindingAck(msg);
        } else if (ev instanceof HandoverInitiate msg) {
            processHandoverInitiate(msg);
        } else if (ev instanceof HandoverAcknowledge msg) {
            processHandoverAck(msg);
        }
    }

    /**
     * Handle event when a node joined the network and wants to bind to an interface.
     *
     * @param src link of joined node.
     * @param ev  the join event.
     */
    protected void processEnterNetwork(SimEnt src, EnterNetwork ev) {
        System.out.printf("[%d] %s: [%s]%n", (int) SimEngine.getTime(), this, ev);

        var interfaceId = ev.getInterfaceId();
        if (_interfaces[interfaceId] != null) {
            // Cannot bind to an interface that's already in use.
            System.err.printf("ERR: %s: cannot bind to interface %d, already in use%n", this, interfaceId);
            return;
        }

        var addr = new NetworkAddr(getInterfaceAddress(interfaceId), 0, 64);
        connectInterface(interfaceId, addr, (Link) src);
    }

    /**
     * Handle event for when a node leaves the network and the interface should be released.
     *
     * @param src link that left the network.
     * @param ev  leave network event.
     */
    protected void processLeaveNetwork(SimEnt src, LeaveNetwork ev) {
        System.out.printf("[%d] %s: [%s] [src=%s]%n", (int) SimEngine.getTime(), this, ev, ev.getSourceAddress());
        disconnectInterface(ev.getSourceAddress().networkId());
    }

    /**
     * Process a IPv6Tunneled message. This is separate function from the regular forwarding so Home Agents can
     * unpack and send the MN.
     *
     * @param ev the tunneled message.
     */
    protected void processTunneledMessage(IPv6Tunneled ev) {
        // Check if this is addressed to us, in that case unwrap it and send along the original message.
        // This happens when an MN is on a foreign network and sends to a CN.
        if (addressedToRouter(ev.destination())) {
            var original = ev.getOriginalPacket();
            System.out.printf("[%d] %s: recv [%s]. Unpack and send [%s]%n", (int) SimEngine.getTime(), this, ev, original);
            forwardMessage(original);
        } else {
            System.out.printf("[%d] %s: recv [%s]. Forwarding%n", (int) SimEngine.getTime(), this, ev);
            forwardMessage(ev);
        }
    }

    /**
     * Handle Router Solicitation messages, they essentially request a Router Advertisement to be sent.
     *
     * @param ev Solicitation message.
     */
    protected void processRouterSolicitation(SimEnt src, RouterSolicitation ev) {
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);

        // Find which link received the solicitation, so we can send back the advertisement to the correct link.
        for (var entry : _routingTable) {
            if (entry.link() == src) {
                var interfaceId = entry.getInterfaceId();
                var interfaceNetwork = getInterfaceAddress(interfaceId);

                // Advertise the network prefix as the current interface id. We don't have link-local addresses for routers,
                // so use the unspecified address for now.
                var msg = new RouterAdvertisement(NetworkAddr.UNSPECIFIED, NetworkAddr.ALL_NODES_MULTICAST, 0, _name, interfaceNetwork);
                send(src, msg, 0);
            }
        }
    }

    /**
     * Process a Router Advertisement message. For regular routers, we basically do nothing, we don't propagate these
     * over the network.
     *
     * @param ev Router Advertisement message.
     */
    protected void processRouterAdvertisement(RouterAdvertisement ev) {
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);
        // Do nothing.
    }

    /**
     * Handle the Router Solicitation for Proxy Advertisement messages.
     * <p>
     * So here we have a node on our network that wants advertisements for a router on a neighboring network. So we send
     * back advertisements we have in our cache.
     *
     * @param src SimEnt solicitation was received from.
     * @param ev  the Router Solicitation for Proxy Advertisement message.
     */
    protected void processRtSolPr(SimEnt src, RtSolPr ev) {
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);
        debugProxyAdvertisements();

        var interfaceName = getInterfaceName(ev.getName(), ev.getInterfaceId());
        var entry = _proxyAdvertisements.get(interfaceName);
        if (entry != null) {
            var msg = new PrRtAdv(NetworkAddr.UNSPECIFIED, ev.source(), 0, entry.advertisement.getName(), entry.advertisement.getNetworkPrefix(), interfaceName);
            send(src, msg, 0);
        }
    }

    /**
     * Handle the Proxy Router Advertisements.
     * <p>
     * For routers if we receive this, as these are probably multicasted, we basically do not propagate these over the
     * network.
     *
     * @param src SimEnt advertisement was received from.
     * @param ev  the Proxy Router Advertisement message.
     */
    protected void processPrRtAdv(SimEnt src, PrRtAdv ev) {
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);

        // Find the interface address that received the message.
        NetworkAddr interfaceAddress = null;
        NetworkAddr from = null;
        for (var entry : _routingTable) {
            if (entry.link() == src) {
                long interfaceNetwork = getInterfaceAddress(entry.getInterfaceId());
                interfaceAddress = new NetworkAddr(interfaceNetwork, 0);
                from = entry.getAddr();
            }
        }

        if (interfaceAddress != null && from != null) {
            var entry = new ProxyAdvertisementEntry(interfaceAddress, from, ev);
            _proxyAdvertisements.put(ev.getInterfaceName(), entry);
        }
    }

    /**
     * Handle the Binding Update message. The router acts as a Home Agent, so if it receives this for an address it
     * manages, it will update the binding cache with the new care of address.
     *
     * @param ev the Binding Update message.
     */
    protected void processBindingUpdate(BindingUpdate ev) {
        if (!addressedToRouter(ev.destination())) {
            forwardMessage(ev);
            return;
        }
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);

        // Addressed to us, so update the care of address.
        updateBindingCache(ev.getHomeAddress(), ev.source());

        var msg = new BindingAck(ev.destination(), ev.source(), 0);
        forwardMessage(msg);
    }

    /**
     * Handle the Binding Ack message. Routers should not receive this, so we just drop them.
     *
     * @param ev Binding Ack message.
     */
    protected void processBindingAck(BindingAck ev) {
        if (!addressedToRouter(ev.destination())) {
            forwardMessage(ev);
        }
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);
        // Do nothing if addressed to us.
    }

    /**
     * Handle the Fast Binding Update messages. These are part of the Fast Mobile IPv6 Handovers, but the message is
     * basically the same as the regular Binding Update, the difference lies in how they are handled.
     *
     * @param ev fast binding update message.
     */
    protected void processFastBindingUpdate(FastBindingUpdate ev) {
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);

        // Intercept the fast binding update, otherwise we cannot know the home agent address.
        var entry = _proxyAdvertisements.get(ev.getInterfaceName());
        if (entry != null) {
            var homeAgentAddress = new NetworkAddr(ev.destination().networkId(), 0);
            var handover = new FastHandover(ev.getSequence(), homeAgentAddress, ev.getHomeAddress(), ev.source(), ev.getNewCareOfAddress());
            var identifier = ev.getSequence();
            _handovers.put(identifier, handover);

            // If this is addressed to the current router, initiate then handover procedure.
            var advEntry = _proxyAdvertisements.get(ev.getInterfaceName());
            var msg = new HandoverInitiate(advEntry.interfaceAddr, advEntry.from, 0, identifier);
            forwardMessage(msg);
        }
    }

    /**
     * Handle the Fast Binding Ack message. These are similar to the regular Binding Ack messages, but routers can
     * receive these and must handle them.
     *
     * @param ev the Fast Binding Ack message.
     */
    protected void processFastBindingAck(FastBindingAck ev) {
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);
        // Do nothing, these should only be sent from the current router to the node.
    }

    /**
     * Handle the Handover Initiate message.
     * <p>
     * These are sent from the current access router to the next access router to initiate a handover for the MN.
     *
     * @param ev handover initiate message.
     */
    protected void processHandoverInitiate(HandoverInitiate ev) {
        if (!addressedToRouter(ev.destination())) {
            forwardMessage(ev);
            return;
        }
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);

        // We don't perform a lot of processing and always accept new nodes!
        var msg = new HandoverAcknowledge(ev.destination(), ev.source(), 0, ev.getIdentifier());
        forwardMessage(msg);
    }

    /**
     * Handle the Handover Acknowledge message.
     * <p>
     * These are sent from the next access router to the current in response to a HandoverInitiate message.
     *
     * @param ev the Handover Acknowledge message
     */
    protected void processHandoverAck(HandoverAcknowledge ev) {
        if (!addressedToRouter(ev.destination())) {
            forwardMessage(ev);
            return;
        }
        System.out.printf("[%d] %s: recv [%s]%n", (int) SimEngine.getTime(), this, ev);

        var handover = _handovers.get(ev.getIdentifier());
        if (handover != null) {
            _handovers.remove(ev.getIdentifier());

            // Check if we are the HA for this MN.
            if (addressedToRouter(handover.homeAgentAddress)) {
                updateBindingCache(handover.homeAddress, handover.nextCareOfAddress);
            } else {
                // Send binding update to HA.
                var BU = new BindingUpdate(handover.nextCareOfAddress, handover.homeAgentAddress, 0, 0, handover.homeAddress);
                System.out.printf("*********** %s: send [%s]%n", this, BU);
                forwardMessage(BU);
            }

            // Send fast binding ack to client. As we just updated the binding to the new care of address. We skip
            // the tunneling check.
            var interfaceLink = getInterface(handover.currentCareOfAddress.networkId());
            var src = getSrcInterfaceAddress(handover.currentCareOfAddress);
            var FBAck = new FastBindingAck(src, handover.currentCareOfAddress, 0);
            send(interfaceLink, FBAck, 0);
        }
    }

    /**
     * Finds the interface based on the destination address and forwards the message to the correct interface.
     *
     * @param ev message to forward.
     */
    protected void forwardMessage(Message ev) {
        if (_bindingCache.containsKey(ev.destination())) {
            var coa = _bindingCache.get(ev.destination());
            System.out.printf("[%d] %s: tunnel [%s] to dst=%s%n", (int) SimEngine.getTime(), this, ev, coa);
            ev = new IPv6Tunneled(ev.destination(), coa, 0, ev);
        }

        SimEnt sendNext = getInterface(ev.destination().networkId());
        if (sendNext == null) {
            _pktsDroppedNoInterface += 1;
            System.out.printf("ERR: %s wants to send to %s but interface is unbound%n", this, ev.destination());
        } else {
            System.out.printf("[%d] %s: forward [%s]%n", (int) SimEngine.getTime(), this, ev);
            send(sendNext, ev, 0);
        }
    }

    /**
     * Send out proxy advertisements to ...
     */
    protected void sendProxyAdvertisements() {
        System.out.printf("[%d] %s: send Proxy Advertisements to all routers%n", (int) SimEngine.getTime(), this);
        for (int i = 0; i < _interfaces.length; ++i) {
            var interfaceName = getInterfaceName(_name, i);
            var interfaceNetwork = getInterfaceAddress(i);

            for (int j = 0; j < _interfaces.length; ++j) {
                var link = _interfaces[j];
                if (link == null || i == j) continue;

                // Send a proxy advertisement for one of the networks, to all other routers on the other links.
                send(link, new PrRtAdv(NetworkAddr.UNSPECIFIED, NetworkAddr.ALL_ROUTER_MULTICAST, 0, _name, interfaceNetwork, interfaceName), 0);
            }
        }
    }

    /**
     * Updates the Home Agent home address to care of address cache.
     *
     * @param homeAddress   The node's home address.
     * @param careOfAddress The node's new care of address.
     */
    private void updateBindingCache(NetworkAddr homeAddress, NetworkAddr careOfAddress) {
        System.out.printf("[%d] %s: update binding cache [home=%s, coa=%s]%n", (int) SimEngine.getTime(), this, homeAddress, careOfAddress);
        _bindingCache.put(homeAddress, careOfAddress);
    }

    /**
     * Checks if the address is one of the router's.
     *
     * @param addr address that may be the router's link address.
     * @return true if this is the router's address.
     */
    private boolean addressedToRouter(NetworkAddr addr) {
        for (int i = 0; i < _interfaces.length; ++i) {
            var networkId = getInterfaceAddress(i);
            if (networkId == addr.networkId() && addr.nodeId() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the interface networkId
     *
     * @param interfaceId interface to get address of.
     * @return the interface's network id.
     */
    private long getInterfaceAddress(int interfaceId) {
        return _baseAddress.networkId() + interfaceId;
    }

    /**
     * Returns the name of a specific interface.
     *
     * @param interfaceId interface to the name of.
     * @return name of interface.
     */
    private String getInterfaceName(String routerName, int interfaceId) {
        return String.format("%s-%d", routerName, interfaceId);
    }

    /**
     * Returns the source address of the outgoing link. So we can get messages sent back to the router.
     *
     * @param dst destination address, so we can figure out which link.
     * @return source address of link.
     */
    private NetworkAddr getSrcInterfaceAddress(NetworkAddr dst) {
        for (var entry : _routingTable) {
            if (entry.getAddr().matches(dst.networkId())) {
                return new NetworkAddr(getInterfaceAddress(entry.getInterfaceId()), 0);
            }
        }
        return null;
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
     * Prints statistics and configuration of router.
     */
    public void onSimulationComplete() {
        System.out.printf("%s%n", this);
        System.out.printf("- Packets dropped (no interface bound): %d%n", _pktsDroppedNoInterface);
        debugRoutingTables();
        debugBindingCache();
        debugProxyAdvertisements();
    }

    /**
     * Pretty prints the routing tables to standard out.
     */
    protected void debugRoutingTables() {
        //System.out.printf("%s Routing table:%n", this);
        System.out.printf("- Routing table:%n");
        for (var entry : _routingTable) {
            System.out.printf("  interface %d -> %s%n", entry.getInterfaceId(), entry.getAddr());
        }
    }

    /**
     * Pretty prints the proxy advertisement cache to standard out.
     */
    protected void debugProxyAdvertisements() {
        //System.out.printf("%s Proxy Advertisement cache:%n", this);
        System.out.printf("- Proxy Advertisement cache:%n");
        for (var entry : _proxyAdvertisements.entrySet()) {
            var interfaceName = entry.getKey();
            var adv = entry.getValue();
            System.out.printf("  %s -> %s%n", interfaceName, new NetworkAddr(adv.advertisement.getNetworkPrefix(), 0));
        }
    }

    /**
     * Pretty prints the binding cache to standard out.
     */
    protected void debugBindingCache() {
        //System.out.printf("%s Binding Cache:%n", this);
        System.out.printf("- Binding cache:%n");
        for (var entry : _bindingCache.entrySet()) {
            System.out.printf("  %s -> %s%n", entry.getKey(), entry.getValue());
        }
    }
}
