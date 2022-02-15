package Sim.Traffic;

import java.util.Random;

/**
 * Traffic Generator that sends packets from a Gaussian distribution.
 */
public class Gaussian extends TrafficGenerator {
    private Random _generator = new Random();

    /**
     * Instantiates a new traffic generator that generates a stream of packets with a delay from a Gaussian distribution.
     *
     * @param network the node's network id.
     * @param node    the node's id.
     */
    public Gaussian(int network, int node) {
        super(network, node);
    }

    /**
     * Calculates and returns the delay until the next packet should be sent. The delay will be a value from a
     * Gaussian distribution.
     *
     * @return delay in milliseconds until the next packet should be sent.
     */
    @Override
    protected int getNextSendTime() {
        double delay = _generator.nextGaussian(100, 15);
        return (int) delay;
    }
}
