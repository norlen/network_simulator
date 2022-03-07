package Sim.Events;

import Sim.Event;
import Sim.NetworkAddr;
import Sim.SimEnt;

public class LeaveNetwork implements Event {
    private final SimEnt _src;
    private final SimEnt _dst;
    private final NetworkAddr _srcAddr;

    public LeaveNetwork(SimEnt src, SimEnt dst, NetworkAddr srcAddr) {
        _src = src;
        _dst = dst;
        _srcAddr = srcAddr;
    }
    
    public SimEnt getSource() {
        return _src;
    }

    public SimEnt getDestination() {
        return _dst;
    }

    public NetworkAddr getSourceAddress() {
        return _srcAddr;
    }

    @Override
    public void entering(SimEnt locale) {

    }
}
