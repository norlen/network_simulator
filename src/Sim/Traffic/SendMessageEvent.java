package Sim.Traffic;

import Sim.Event;
import Sim.NetworkAddr;
import Sim.SimEnt;

public class SendMessageEvent implements Event {
    public NetworkAddr _dst;
    public double _delay;

    public SendMessageEvent(NetworkAddr dst, double delay) {
        _dst = dst;
        _delay = delay;
    }

    @Override
    public void entering(SimEnt locale) {

    }
}
