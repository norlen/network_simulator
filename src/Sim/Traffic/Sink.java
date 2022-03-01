package Sim.Traffic;

import Sim.Event;
import Sim.SimEnt;

/**
 * Traffic sink that processes incoming messages.
 */
public interface Sink {
    public void process(SimEnt src, Event ev);
}
