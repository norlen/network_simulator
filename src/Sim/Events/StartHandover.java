package Sim.Events;

import Sim.Event;
import Sim.SimEnt;

public class StartHandover implements Event {
    private final SimEnt _router;
    private final String _nextAccessRouter;
    private final int _nextInterfaceId;

    public StartHandover(SimEnt router, String nextAccessRouter, int nextInterfaceId) {
        _router = router;
        _nextAccessRouter = nextAccessRouter;
        _nextInterfaceId = nextInterfaceId;
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

    @Override
    public void entering(SimEnt locale) {

    }
}
