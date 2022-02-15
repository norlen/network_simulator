package Sim.Traffic;

import java.util.Random;

/**
 * Traffic Generator that sends packets from a Gaussian distribution.
 */
public class Gaussian extends TrafficGenerator {
    private Random _generator = new Random();

    // Mean of the Gaussian distribution.
    private final double _mean;

    // Standard deviation of the Gaussian distribution.
    private final double _stddev;

    /**
     * Instantiates a new traffic generator that generates a stream of packets with a delay from a Gaussian distribution.
     *
     * @param network the node's network id.
     * @param node    the node's id.
     * @param mean    mean of the Gaussian distribution.
     * @param stddev  standard deviation of the Gaussian distribution.
     */
    public Gaussian(int network, int node, double mean, double stddev) {
        super(network, node);
        _mean = mean;
        _stddev = stddev;
    }

    /**
     * Calculates and returns the delay until the next packet should be sent. The delay will be a value from a
     * Gaussian distribution.
     *
     * @return delay in milliseconds until the next packet should be sent.
     */
    @Override
    protected double getNextSendTime() {
        double delay = _generator.nextGaussian(_mean, _stddev);
        return (int) delay;
    }
}
