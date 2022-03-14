package Sim.Events;

import Sim.Event;
import Sim.SimEnt;

public class StartHandover implements Event {
    private final SimEnt _router;
    private final String _nextAccessRouter;
    private final int _nextInterfaceId;
    private boolean _fastHandover;

    public StartHandover(SimEnt router, int nextInterfaceId) {
        _router = router;
        _nextAccessRouter = null;
        _nextInterfaceId = nextInterfaceId;
        _fastHandover = false;
    }

    public StartHandover(SimEnt router, String nextAccessRouter, int nextInterfaceId) {
        _router = router;
        _nextAccessRouter = nextAccessRouter;
        _nextInterfaceId = nextInterfaceId;
        _fastHandover = true;
    }

    public SimEnt getRouter() {
        return _router;
    }

    public String getNextAccessRouter() {
        return _nextAccessRouter;
    }

    public int getNextInterfaceId() {
        return _nextInterfaceId;
    }

    public boolean isFastHandover() {
        return _fastHandover;
    }

    @Override
    public void entering(SimEnt locale) {

    }

    @Override
    public String toString() {
        return String.format("StartHandover Event, fast=%b, router=%s, interfaceId=%d", _fastHandover, _router, _nextInterfaceId);
    }
}
