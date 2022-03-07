package Sim.Events;

import Sim.Event;
import Sim.SimEnt;

public class EnterNetwork implements Event {
    private final SimEnt _src;
    private final int _interfaceId;
    private final int _networkId;

    public EnterNetwork(SimEnt src, int interfaceId, int networkId) {
        _src = src;
        _interfaceId = interfaceId;
        _networkId = networkId;
    }

    public SimEnt getSource() {
        return _src;
    }

    public int getInterfaceId() {
        return _interfaceId;
    }

    public int getNetworkId() {
        return _networkId;
    }

    @Override
    public void entering(SimEnt locale) {

    }
}
