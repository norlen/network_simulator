package Sim;

import java.util.Random;

/**
 * LossyLink is a Link that can drop packets, introduce delay for each packet, and have jitter so the delay for each
 * packet differs.
 * <p>
 * The delay is modelled such that the total delay for a packet is the delay added with a random value evenly
 * distributed in [-jitter, jitter).
 */
public class LossyLink extends Link {
    // The amount of time to delay each packet.
    private final double _delay;

    // How much jitter to add to each packet.
    private final double _jitter;

    // Probability that a packet should be dropped.
    private final double _dropProbability;

    // Number of packets this link has dropped.
    private int _numDroppedPackets = 0;

    // Random number generator to randomize jitter.
    private final Random _generator;

    /**
     * Instantiates a new LossyLink with the given settings.
     *
     * @param delay           base delay for each packet.
     * @param jitter          jitter for each packet, time added or removed from each packet.
     * @param dropProbability probability that a packet is dropped in [0, 1].
     */
    public LossyLink(double delay, double jitter, double dropProbability) {
        super();
        _delay = delay;
        _jitter = jitter;
        _dropProbability = dropProbability;
        _generator = new Random();
    }

    /**
     * Returns the number of dropped packets since the link was created.
     *
     * @return the number of dropped packets.
     */
    public int getNumDroppedPackets() {
        return _numDroppedPackets;
    }

    /**
     * Handles receiving an event. Currently, only handles incoming messages.
     * <p>
     * When handling a message it introduces a delay and jitter, or drops the packet based off the values passed during
     * construction. It also updates the jitter estimate.
     *
     * @param src SimEnt that sent the event.
     * @param ev  incoming event.
     */
    @Override
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof Message) {
            if (shouldDropPacket()) {
                System.out.println("== Link drop packet: ");
                _numDroppedPackets += 1;
                return;
            }

            double delay = getDelay();
            //System.out.println("-- Link recv msg, delay: " + delay);
            _now = (int) delay;
        }
        super.recv(src, ev);
    }

    /**
     * Checks if a packet should be dropped.
     * <p>
     * Generates a random value and compares to the drop probability.
     *
     * @return true if the packet should be dropped, false otherwise.
     */
    private boolean shouldDropPacket() {
        return _generator.nextDouble() < _dropProbability;
    }

    /**
     * Calculates the delay for a single packet. Adds delay and jitter to the time.
     *
     * @return the delay for a packet in milliseconds.
     */
    private double getDelay() {
        double jitter = _jitter * 2 * _generator.nextDouble() - _jitter;
        return _delay + jitter;
    }
}
