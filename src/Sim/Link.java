package Sim;

import Sim.Events.Connected;
import Sim.Events.Disconnected;
import Sim.Events.EnterNetwork;
import Sim.Events.LeaveNetwork;

// This class implements a link without any loss, jitter or delay
public class Link extends SimEnt {
    protected SimEnt _connectorA = null;
    protected SimEnt _connectorB = null;

    // Delay until a message is forwarded on the link.
    protected double _now = 0;

    // If the node's link is connected has connected to a valid interface.
    protected boolean _enabled = true;

    public Link() {
        super();
    }

    // Connects the link to some simulation entity like
    // a node, switch, router etc.
    public void setConnector(SimEnt connectTo) {
        if (_connectorA == null) {
            _connectorA = connectTo;
        } else {
            _connectorB = connectTo;
        }

        System.out.printf("** Link setConnector(), connectorA: %s, connectorB: %s%n", _connectorA, _connectorB);
        if (_connectorA != null) {
            if (_connectorA == null) {
                // Disconnected.
                //_enabled = false;
                send(_connectorA, new Disconnected(), 0);
            } else {
                // _enabled = true;
                send(_connectorA, new Connected(), 0);
            }
        }
    }

    // Called when a message enters the link
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof Message || ev instanceof EnterNetwork || ev instanceof LeaveNetwork) {
            forward(src, ev);
        }
    }

    protected void forward(SimEnt src, Event ev) {
        if (!_enabled) {
            System.err.println("Link recv msg, dropping since link is disabled");
            return;
        }

        System.out.println("Link recv msg, passes it through");
        if (src == _connectorA) {
            send(_connectorB, ev, _now);
        } else {
            send(_connectorA, ev, _now);
        }
        _now = 0;
    }
}