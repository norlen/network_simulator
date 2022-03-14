package Sim.Events;

import Sim.Event;
import Sim.SimEnt;

public class Disconnected implements Event {
    @Override
    public void entering(SimEnt locale) {

    }

    @Override
    public String toString() {
        return String.format("Disconnected event");
    }
}
