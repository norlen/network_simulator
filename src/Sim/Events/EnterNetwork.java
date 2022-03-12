package Sim.Events;

import Sim.Event;
import Sim.SimEnt;

public class EnterNetwork implements Event {
    private final SimEnt _src;
    private final SimEnt _router;
    private final int _interfaceId;

    public EnterNetwork(SimEnt src, SimEnt router, int interfaceId) {
        _src = src;
        _router = router;
        _interfaceId = interfaceId;
    }

    public SimEnt getSource() {
        return _src;
    }

    public SimEnt getRouter() {
        return _router;
    }

    public int getInterfaceId() {
        return _interfaceId;
    }

    @Override
    public void entering(SimEnt locale) {

    }
}
