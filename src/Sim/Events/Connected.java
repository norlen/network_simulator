package Sim.Events;

import Sim.Event;
import Sim.SimEnt;

public class Connected implements Event {
    public Connected() {

    }

    @Override
    public void entering(SimEnt locale) {

    }

    @Override
    public String toString() {
        return String.format("Connected event");
    }
}
