package Sim.Traffic;

import java.util.Random;

/**
 * Traffic Generator that sends packets with a delay from a Poisson distribution.
 */
public class Poisson extends TrafficGenerator {
    private final Random _generator = new Random();

    // Lambda value when sampling the Poisson distribution.
    private final int _lambda;

    /**
     * Instantiates a new traffic generator that generates a stream of packets with a delay from a Poisson distribution.
     *
     * @param lambda lambda value when sampling the Poisson distribution.
     */
    public Poisson(int packetsToSend, int lambda) {
        super(packetsToSend);
        _lambda = lambda;
    }

    /**
     * Generate a random delay in a Poisson distribution. Uses the Knuth algorithm with pseudocode available at:
     * https://en.wikipedia.org/wiki/Poisson_distribution
     *
     * @return a random delay in a Poisson distribution.
     */
    @Override
    public double getNextSendTime() {
        double L = Math.exp(-_lambda);
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
