package Sim;

import java.util.Random;

/**
 * LossyLink is a Link that can drop packets, introduce delay for each packet, and have jitter so the delay for each
 * packet differs.
 * <p>
 * The delay is modelled such that the total delay for a packet is the delay added with a random value evenly
 * distributed in [0, jitter). This makes the delay the best case delay, i.e. the network is free for only us to use.
 */
public class LossyLink extends Link {
    // The amount of time to delay each packet.
    private final double _delay;

    // How much jitter to add to each packet.
    private final double _jitter;

    // Probability that a packet should be dropped.
    private final double _dropProbability;

    // Estimated jitter calculated using the algorithm described in RFC 1889.
    private double _estimatedJitter = 0.0;

    // Difference in time between packet sent and current time for the last packet.
    private double _transit = 0.0;

    // Random number generator to randomize jitter.
    private final Random _generator;

    /**
     * Instantiates a new LossyLink with the given settings.
     *
     * @param delay           best case delay for each packet.
     * @param jitter          random value added to the delay.
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
     * Handles receiving an event. Currently, only handles incoming messages.
     * <p>
     * When handling a message it introduces a delay and jitter, or drops the packet based off the values passed during
     * construction. It also updates the jitter estimate.
     *
     * @param src SimEnt that sent the event.
     * @param ev  incoming event.
     */
    public void recv(SimEnt src, Event ev) {
        if (ev instanceof Message) {
            if (shouldDropPacket()) {
                System.out.println("== Link drop packet: ");
                return;
            }

            updateJitterEstimation(SimEngine.getTime(), ((Message) ev).getTimestamp());
            System.out.println("Link estimated jitter: " + _estimatedJitter);

            double delay = getDelay();
            System.out.println("-- Link recv msg, delay: " + delay);
            if (src == _connectorA) {
                send(_connectorB, ev, delay);
            } else {
                send(_connectorA, ev, delay);
            }
        }
    }

    /**
     * Checks if a packet should be dropped.
     * <p>
     * Generates a random value and compares to the drop probability.
     *
     * @return true if the packet should be dropped, false otherwise.
     */
    private boolean shouldDropPacket() {
        double roll = _generator.nextDouble();
        return roll < _dropProbability;
    }

    /**
     * Calculates the delay for a single packet. Adds both the best case delay and jitter.
     *
     * @return the delay for a packet in milliseconds.
     */
    private double getDelay() {
        double jitter = _generator.nextDouble() * _jitter;
        return _delay + jitter;
    }

    /**
     * Updates the jitter estimation using algorithm described in RFC 1889, under section A.8 Estimating the
     * Interarrival Jitter (https://datatracker.ietf.org/doc/html/rfc1889#appendix-A.8).
     *
     * @param arrivalTime     the current time.
     * @param packetTimestamp timestamp of the incoming packet.
     */
    private void updateJitterEstimation(double arrivalTime, double packetTimestamp) {
        double transit = arrivalTime - packetTimestamp;
        double d = transit - _transit;
        _transit = transit;
        _estimatedJitter += (1.0 / 16.0) * (Math.abs(d) - _estimatedJitter);
    }
}
