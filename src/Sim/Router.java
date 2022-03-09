package Sim;

import Sim.Events.EnterNetwork;
import Sim.Events.LeaveNetwork;
import Sim.Messages.ICMPv6.RouterAdvertisement;
import Sim.Messages.ICMPv6.RouterSolicitation;
import Sim.Messages.IPv6Tunneled;
import Sim.Messages.MobileIPv6.BindingUpdate;

public class Router extends SimEnt {
    private final RouteTableEntry[] _routingTable;
    private final int _interfaces;
    private final int _now = 0;
    private final int _baseNetwork;

    // When created, number of interfaces are defined
    public Router(int interfaces, int baseNetwork) {
        _routingTable = new RouteTableEntry[interfaces];
        _interfaces = interfaces;
        _baseNetwork = baseNetwork;
    }

    // This method connects links to the router and also informs the
    // router of the host connects to the other end of the link
    public void connectInterface(int interfaceNumber, Link link, int[] networks) {
        if (interfaceNumber < _interfaces) {
            _routingTable[interfaceNumber] = new RouteTableEntry(networks, link);
        } else {
            System.out.println("Trying to connect to port not in router");
        }

        link.setConnector(this);
    }

    /**
     * Disconnect the interface having a certain network id.
     *
     * @param networkId
     */
    public void disconnectInterface(int networkId) {
        for (int i = 0; i < _routingTable.length; i++) {
            if (_routingTable[i] == null) continue;
            System.out.println(_routingTable[i]);

            for (int j = 0; j < _routingTable[i].getNetworkIds().length; ++j) {
                if (_routingTable[i].getNetworkIds()[j] == networkId) {
                    // If this is a link, then disconnect it.
                    if (_routingTable[i].link() instanceof Link link) {
                        link.setConnector(null);
                    }

                    // Clear routing table entry.
                    _routingTable[i] = null;
                    return;
                }
            }
        }
    }

    // This method searches for an entry in the routing table that matches
    // the network number in the destination field of a messages. The link
    // represents that network number is returned
    private SimEnt getInterface(int networkAddress) {
        SimEnt routerInterface = null;
        for (int i = 0; i < _interfaces; i++) {
            if (_routingTable[i] == null) continue;

            for (int j = 0; j < _routingTable[i].getNetworkIds().length; ++j) {
                if (_routingTable[i].getNetworkIds()[j] == networkAddress) {
                    routerInterface = _routingTable[i].link();
                }
            }
        }
        return routerInterface;
    }

    // When messages are received at the router this method is called
    @Override
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof EnterNetwork event) {
            System.out.println("== Router handles EnterNetwork");
            processEnterNetwork(src, event);
        } else if (ev instanceof LeaveNetwork event) {
            System.out.println("== Router handles LeaveNetwork");
            processLeaveNetwork(src, event);
        } else if (ev instanceof Message message) {
            System.out.println("Router handles packet with seq: " + message.seq() + " from node: " + message.source());
            if (ev instanceof RouterSolicitation event) {
                processRouterSolicitation(event);
            } else if (ev instanceof RouterAdvertisement) {
                // Don't pass these along.
            } else if (ev instanceof BindingUpdate event) {
                processBindingUpdate(event);
            } else if (ev instanceof IPv6Tunneled event) {
                var original = event.getOriginalPacket();
                forwardMessage(original);
            } else {
                forwardMessage(message);
            }
        }
    }

    protected void processEnterNetwork(SimEnt src, EnterNetwork ev) {
        var interfaceId = ev.getInterfaceId();
        if (_routingTable[interfaceId] != null) {
            // Cannot bind to an interface that's already in use.
            System.err.printf("Cannot bind to interface %d: already in use%n", interfaceId);
            return;
        }
        System.out.println("=========================== enter network");

        connectInterface(interfaceId, (Link) src, new int[]{_baseNetwork + interfaceId});
    }

    protected void processLeaveNetwork(SimEnt src, LeaveNetwork ev) {
        disconnectInterface(ev.getSourceAddress().networkId());
    }

    protected void processRouterSolicitation(RouterSolicitation ev) {
        for (int i = 0; i < _routingTable.length; ++i) {
            if (_routingTable[i] == null) continue;

            // Advertise the network prefix as the current interface id.
            var next = _routingTable[i].link();
            var msg = new RouterAdvertisement(null, null, 0, _baseNetwork + i);
            send(next, msg, 0);
        }
    }

    protected void processBindingUpdate(BindingUpdate ev) {
        forwardMessage(ev);
    }

    protected void forwardMessage(Message ev) {
        SimEnt sendNext = getInterface(ev.destination().networkId());
        if (sendNext == null) {
            System.out.printf("==== Router wants to send to %d but interface is unbound%n", ev.destination().networkId());
        } else {
            System.out.println("Router sends to node: " + ev.destination().networkId() + "." + ev.destination().nodeId());
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
}
