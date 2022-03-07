package Sim.Events;

import Sim.Event;
import Sim.NetworkAddr;
import Sim.SimEnt;

public class UpdateInterface implements Event {
    private final SimEnt _src;
    private final NetworkAddr _addr;
    private final int _newInterfaceId;
    private final int _networkId;

    public UpdateInterface(SimEnt src, NetworkAddr addr, int newInterfaceId, int networkId) {
        _src = src;
        _addr = addr;
        _newInterfaceId = newInterfaceId;
        _networkId = networkId;
    }

    public NetworkAddr getAddr() {
        return _addr;
    }

    public int getNewInterfaceId() {
        return _newInterfaceId;
    }

    public int getNetworkId() {
        return _networkId;
    }

    @Override
    public void entering(SimEnt locale) {

    }

    public SimEnt getSrc() {
        return _src;
    }
}
