package Sim.Events;

import Sim.Event;
import Sim.SimEnt;

public class Connected implements Event {
    private final int _networkId;
    private final SimEnt _peer;

    public Connected(int networkId) {
        _networkId = networkId;
    }

    @Override
    public void entering(SimEnt locale) {

    }
}
