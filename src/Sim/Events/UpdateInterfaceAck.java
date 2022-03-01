package Sim.Events;

import Sim.Event;
import Sim.SimEnt;

public class UpdateInterfaceAck implements Event {
    private final boolean _changedInterface;
    private final int _newInterfaceId;
    private final int _networkId;

    public UpdateInterfaceAck(boolean changedInterface, int newInterfaceId, int networkId) {
        _changedInterface = changedInterface;
        _newInterfaceId = newInterfaceId;
        _networkId = networkId;
    }

    public boolean getChangedInterface() {
        return _changedInterface;
    }

    @Override
    public void entering(SimEnt locale) {

    }

    public int getInterfaceId() {
        return _newInterfaceId;
    }

    public int getNetworkId() {
        return _networkId;
    }
}
