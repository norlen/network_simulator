package Sim;

// This class implements a simple router

import Sim.Events.UpdateInterface;
import Sim.Events.UpdateInterfaceAck;

public class Router extends SimEnt {
    private RouteTableEntry[] _routingTable;
    private int _interfaces;
    private int _now = 0;

    // When created, number of interfaces are defined
    Router(int interfaces) {
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
    public void recv(SimEnt source, Event event) {
        if (event instanceof Message ev) {
            System.out.println("Router handles packet with seq: " + ev.seq() + " from node: " + ev.source().networkId() + "." + ev.source().nodeId());
            SimEnt sendNext = getInterface(((Message) event).destination().networkId());
            System.out.println("Router sends to node: " + ev.destination().networkId() + "." + ev.destination().nodeId());
            send(sendNext, event, _now);
        }

        if (event instanceof UpdateInterface ev) {
            System.out.println("== Router received a ChangeInterface packet from " + ev.getAddr() + " change interface to " + ev.getNewInterfaceId());
            
            var success = updateInterface(ev.getAddr(), ev.getNewInterfaceId());
            var sendNext = getInterface(ev.getAddr().networkId());
            var msg = new UpdateInterfaceAck(success, ev.getNewInterfaceId(), ev.getNetworkId());

            send(sendNext, msg, _now);
        }
    }

    private boolean updateInterface(NetworkAddr address, int newInterfaceId) {
        if (_routingTable[newInterfaceId] != null) {
            // Interface ID already in use.
            return false;
        }

        for (int i = 0; i < _routingTable.length; i++) {
            if (_routingTable[i] == null) continue;

            var node = (Node) _routingTable[i].node();
            if (node.getAddr().networkId() == address.networkId()) {
                _routingTable[newInterfaceId] = _routingTable[i];
                _routingTable[i] = null;
                return true;
            }
        }

        // Old interface not found.
        return false;
    }
}
