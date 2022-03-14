package Sim.Traffic;


import Sim.Event;
import Sim.SimEnt;

import java.util.ArrayList;

/**
 * Simple sink class that just counts the number of packets received.
 * <p>
 * It can keep track of multiple counters, and new counters can be added as necessary.
 */
public class CountingSink implements Sink {
    // All counts we keep track of.
    private final ArrayList<Integer> _counts = new ArrayList<>();

    // Names for the counts.
    private final ArrayList<String> _names = new ArrayList<>();

    // Which count we should increment.
    private int _currentIdx = 0;

    /**
     * Instantiate a new CountingSink. By default, one count is initialized, so just call `newCount` when an additional
     * one is needed.
     */
    public CountingSink() {
        _counts.add(0);
        _names.add("Default");
    }

    @Override
    public void process(SimEnt src, Event ev) {
        int count = _counts.get(_currentIdx);
        _counts.set(_currentIdx, count + 1);
    }

    /**
     * Adds a new counter, initialized to zero.
     */
    public void newCount(String identifier) {
        _names.add(identifier);
        _counts.add(0);
        _currentIdx += 1;
    }

    /**
     * Print all the counts gathered.
     */
    public void printCounts() {
        System.out.printf("- Sink stats:%n");
        for (int i = 0; i < _counts.size(); ++i) {
            System.out.printf("  %s: %d%n", _names.get(i), _counts.get(i));
        }
    }
}
