package Sim.Events;

import Sim.Event;
import Sim.NetworkAddr;
import Sim.SimEnt;

public class LeaveNetwork implements Event {
    private final NetworkAddr _srcAddr;

    public LeaveNetwork(NetworkAddr srcAddr) {
        _srcAddr = srcAddr;
    }

    public NetworkAddr getSourceAddress() {
        return _srcAddr;
    }

    @Override
    public void entering(SimEnt locale) {

    }
}
