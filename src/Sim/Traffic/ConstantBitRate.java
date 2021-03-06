package Sim.Traffic;

/**
 * Constant bit rate traffic generator. Sends traffic with just one specific delay between each packet.
 */
public class ConstantBitRate extends TrafficGenerator {
    // How much time between each packet.
    private final int _timeBetweenSending;

    /**
     * Creates a new traffic generator that generates a stream of packets with a specific delay between each packet.
     *
     * @param timeBetweenSending delay between each sent packet.
     */
    public ConstantBitRate(int packetsToSend, int timeBetweenSending) {
        super(packetsToSend);
        _timeBetweenSending = timeBetweenSending;
    }

    /**
     * Returns the time between each packet. This will always be the same.
     *
     * @return Delay until the next packet should be sent.
     */
    @Override
    public double getNextSendTime() {
        return _timeBetweenSending;
    }
}
