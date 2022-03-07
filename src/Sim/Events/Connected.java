package Sim.Events;

import Sim.Event;
import Sim.SimEnt;

public class Connected implements Event {
    private final int _networkId;

    public Connected(int networkId) {
        _networkId = networkId;
    }

    public int getNetworkId() {
        return _networkId;
    }

    @Override
    public void entering(SimEnt locale) {

    }
}
