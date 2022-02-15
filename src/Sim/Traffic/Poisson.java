package Sim.Traffic;

import java.util.Random;

/**
 * Traffic Generator that sends packets with a delay from a Poisson distribution.
 */
public class Poisson extends TrafficGenerator {
    private Random _generator = new Random();

    /**
     * Instantiates a new traffic generator that generates a stream of packets with a delay from a Poisson distribution.
     *
     * @param network the node's network id.
     * @param node    the node's id.
     */
    public Poisson(int network, int node) {
        super(network, node);
    }

    /**
     * Generate a random delay in a Poisson distribution. Uses the Knuth algorithm with pseudocode available at:
     * https://en.wikipedia.org/wiki/Poisson_distribution
     *
     * @return a random delay in a Poisson distribution.
     */
    @Override
    protected int getNextSendTime() {
        double lambda = 100;
        double L = Math.exp(-lambda);
        int k = 0;
        double p = 1;

        do {
            k = k + 1;
            double u = _generator.nextDouble();
            p = p * u;
        } while (p > L);

        return k - 1;
    }
}
