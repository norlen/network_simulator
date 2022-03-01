package Sim.Traffic;

import java.util.Random;

/**
 * Traffic Generator that sends packets from a Gaussian distribution.
 */
public class Gaussian extends TrafficGenerator {
    private final Random _generator = new Random();

    // Mean of the Gaussian distribution.
    private final double _mean;

    // Standard deviation of the Gaussian distribution.
    private final double _stddev;

    /**
     * Instantiates a new traffic generator that generates a stream of packets with a delay from a Gaussian distribution.
     *
     * @param mean   mean of the Gaussian distribution.
     * @param stddev standard deviation of the Gaussian distribution.
     */
    public Gaussian(int packetsToSend, double mean, double stddev) {
        super(packetsToSend);
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
    public double getNextSendTime() {
        return _generator.nextGaussian(_mean, _stddev);
    }
}
