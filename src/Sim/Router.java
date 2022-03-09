package Sim;

import Sim.Events.EnterNetwork;
import Sim.Events.LeaveNetwork;
import Sim.Messages.ICMPv6.RouterAdvertisement;
import Sim.Messages.ICMPv6.RouterSolicitation;
import Sim.Messages.IPv6Tunneled;
import Sim.Messages.MobileIPv6.BindingUpdate;

import java.util.ArrayList;
import java.util.Collections;

public class Router extends SimEnt {
    // Routing table, which holds a prefix which corresponds to an interface.
    private ArrayList<RouteTableEntry> _routingTable;

    // Notes if the interfaces are currently in use.
    private final SimEnt[] _interfaces;

    // The router's base prefix which it uses to generate networks on its interfaces.
    private final long _basePrefix;

    private final int _now = 0;
    private final String _name;


    // When created, number of interfaces are defined
    public Router(String name, int interfaces, long basePrefix) {
        _routingTable = new ArrayList<>();
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

    // When messages are received at the router this method is called
    @Override
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof EnterNetwork event) {
            System.out.printf("== %s handle EnterNetwork%n", this);
            processEnterNetwork(src, event);
        } else if (ev instanceof LeaveNetwork event) {
            System.out.printf("== %s handles LeaveNetwork%n", this);
            processLeaveNetwork(src, event);
        } else if (ev instanceof Message message) {
            System.out.printf("%s handles packet with seq: %d from addr: %s%n", this, message.seq(), message.source());
            if (ev instanceof RouterSolicitation event) {
                processRouterSolicitation(event);
            } else if (ev instanceof RouterAdvertisement) {
                // Don't pass these along.
            } else if (ev instanceof BindingUpdate event) {
                processBindingUpdate(event);
            } else if (ev instanceof IPv6Tunneled event) {
                processTunneledMessage(event);
            } else {
                forwardMessage(message);
            }
        }
    }

    protected void processTunneledMessage(IPv6Tunneled event) {
        forwardMessage(event);
    }

    protected void processEnterNetwork(SimEnt src, EnterNetwork ev) {
        var interfaceId = ev.getInterfaceId();
        if (_interfaces[interfaceId] != null) {
            // Cannot bind to an interface that's already in use.
            System.err.printf("ERR: %s: cannot bind to interface %d, already in use%n", this, interfaceId);
            return;
        }

        connectInterface(interfaceId, _basePrefix + interfaceId, 64, (Link) src);
    }

    protected void processLeaveNetwork(SimEnt src, LeaveNetwork ev) {
        disconnectInterface(ev.getSourceAddress().networkId());
    }

    protected void processRouterSolicitation(RouterSolicitation ev) {
        for (int i = 0; i < _interfaces.length; ++i) {
            var link = _interfaces[i];
            if (link == null) continue;

            // Generate a new network for something that connected.
            long network = _basePrefix + i;

            // Advertise the network prefix as the current interface id.
            var msg = new RouterAdvertisement(null, null, 0, network);
            send(link, msg, 0);
        }
    }

    protected void processBindingUpdate(BindingUpdate ev) {
        forwardMessage(ev);
    }

    protected void forwardMessage(Message ev) {
        SimEnt sendNext = getInterface(ev.destination().networkId());
        if (sendNext == null) {
            System.out.printf("ERR: %s wants to send to %s but interface is unbound%n", this, ev.destination());
        } else {
            System.out.printf("%s sends to %s%n", this, ev.destination());
            send(sendNext, ev, _now);
        }
    }

    /**
     * Checks if this router is a home agent. Used when sending Router Advertisements which has if it's a home agent
     * or not.
     *
     * @return true if the router is a home agent.
     */
    protected boolean isHomeAgent() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("Router %s", _name);
    }

    protected void debugRoutingTables() {
        System.out.printf("%s Routing table:%n", this);
        for (var entry : _routingTable) {
            System.out.printf("  networkId: %h, bits: %d%n -> interface: %d%n", entry.getNetworkId(), entry.getNumBits(), entry.getInterfaceId());
        }
    }
}
