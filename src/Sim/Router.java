package Sim;

// This class implements a simple router

import Sim.Events.Connected;
import Sim.Events.Disconnected;
import Sim.Events.EnterNetwork;
import Sim.Events.LeaveNetwork;
import Sim.Messages.ICMPv6.RouterAdvertisement;
import Sim.Messages.ICMPv6.RouterSolicitation;
import Sim.Messages.MobileIPv6.BindingUpdate;

public class Router extends SimEnt {
    private final RouteTableEntry[] _routingTable;
    private final int _interfaces;
    private final int _now = 0;

    // When created, number of interfaces are defined
    public Router(int interfaces) {
        _routingTable = new RouteTableEntry[interfaces];
        _interfaces = interfaces;
    }

    // This method connects links to the router and also informs the
    // router of the host connects to the other end of the link
    public void connectInterface(int interfaceNumber, int networkId, Link link) {
        if (interfaceNumber < _interfaces) {
            _routingTable[interfaceNumber] = new RouteTableEntry(networkId, link);
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

            if (_routingTable[i].getNetworkId() == networkId) {
                _routingTable[i] = null;
            }
        }
    }

    // This method searches for an entry in the routing table that matches
    // the network number in the destination field of a messages. The link
    // represents that network number is returned
    private SimEnt getInterface(int networkAddress) {
        SimEnt routerInterface = null;
        for (int i = 0; i < _interfaces; i++) {
            if (_routingTable[i] != null) {
                if (_routingTable[i].getNetworkId() == networkAddress) {
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
            } else if (ev instanceof BindingUpdate event) {
                processBindingUpdate(event);
//            } else if (ev instanceof IPv6Tunneled event) {
//                var originalMessage = event.getOriginalPacket();
//                forwardMessage(originalMessage);
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

        connectInterface(interfaceId, ev.getNetworkId(), (Link) src);
        send(src, new Connected(ev.getNetworkId()), 0);
    }

    protected void processLeaveNetwork(SimEnt src, LeaveNetwork ev) {
        disconnectInterface(ev.getSourceAddress().networkId());
        send(src, new Disconnected(), 0);
    }

    protected void processRouterSolicitation(RouterSolicitation ev) {
        var msg = new RouterAdvertisement(null, null, 0);
        multicastMessage(msg, 0);
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

    void multicastMessage(Message msg, int delayExecution) {
        for (var entry : _routingTable) {
            if (entry != null) {
                System.out.println("<< " + entry.link());
                send(entry.link(), msg, delayExecution);
            }
        }
    }
}
