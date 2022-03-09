package Sim.Events;

import Sim.Event;
import Sim.SimEnt;

public class EnterNetwork implements Event {
    private final SimEnt _src;
    private final int _interfaceId;

    public EnterNetwork(SimEnt src, int interfaceId) {
        _src = src;
        _interfaceId = interfaceId;
    }

    public SimEnt getSource() {
        return _src;
    }

    public int getInterfaceId() {
        return _interfaceId;
    }

    @Override
    public void entering(SimEnt locale) {

    }
}
