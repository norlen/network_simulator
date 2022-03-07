package Sim;

// This class implements a simple router

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
    public void connectInterface(int interfaceNumber, SimEnt link, SimEnt node) {
        if (interfaceNumber < _interfaces) {
            _routingTable[interfaceNumber] = new RouteTableEntry(link, node);
        } else {
            System.out.println("Trying to connect to port not in router");
        }
        ((Link) link).setConnector(this);
    }

    // This method searches for an entry in the routing table that matches
    // the network number in the destination field of a messages. The link
    // represents that network number is returned
    private SimEnt getInterface(int networkAddress) {
        SimEnt routerInterface = null;
        for (int i = 0; i < _interfaces; i++) {
            if (_routingTable[i] != null) {
                if (((Node) _routingTable[i].node()).getAddr().networkId() == networkAddress) {
                    routerInterface = _routingTable[i].link();
                }
            }
        }
        return routerInterface;
    }

    // When messages are received at the router this method is called
    public void recv(SimEnt source, Event ev) {
        if (ev instanceof EnterNetwork event) {
            processEnterNetwork(event);
        } else if (ev instanceof LeaveNetwork event) {
            processLeaveNetwork(event);
        } else if (ev instanceof Message message) {
            System.out.println("Router handles packet with seq: " + message.seq() + " from node: " + message.source());
            if (ev instanceof RouterSolicitation event) {
                processRouterSolicitation(event);
            } else if (ev instanceof BindingUpdate event) {
                processBindingUpdate(event);
            } else {
                forwardMessage(message);
            }
        }
    }

    protected void processEnterNetwork(EnterNetwork ev) {
        var interfaceId = ev.getInterfaceId();
        if (_routingTable[interfaceId] != null) {
            // Cannot bind to an interface that's already in use.
            System.err.printf("Cannot bind to interface %d: already in use%n", interfaceId);
            return;
        }

        connectInterface(interfaceId, ev.getLink(), ev.getSource());
    }

    protected void processLeaveNetwork(LeaveNetwork ev) {
        for (int i = 0; i < _routingTable.length; i++) {
            if (_routingTable[i] == null) continue;

            var node = (Node) _routingTable[i].node();
            if (node.getAddr().networkId() == ev.getSourceAddress().networkId()) {
                _routingTable[i] = null;
            }
        }
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
        System.out.println("Router sends to node: " + ev.destination().networkId() + "." + ev.destination().nodeId());
        send(sendNext, ev, _now);
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

    void sendMessage(Message msg, int delayExecution) {
        var link = getInterface(msg.destination().networkId());
        send(link, msg, delayExecution);
    }

    void multicastMessage(Message msg, int delayExecution) {
        for (var entry : _routingTable) {
            if (entry != null) {
                send(entry.link(), msg, delayExecution);
            }
        }
    }
}
