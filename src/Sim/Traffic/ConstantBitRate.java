package Sim.Traffic;

/**
 * Constant bit rate traffic generator. Sends traffic with just one specific delay between each packet.
 */
public class ConstantBitRate extends TrafficGenerator {
    // How much time between each packet.
    private int _timeBetweenSending = 10;

    /**
     * Creates a new traffic generator that generates a stream of packets with a specific delay between each packet.
     *
     * @param network            The node's network id.
     * @param node               The node's id.
     * @param timeBetweenSending delay between each sent packet.
     */
    public ConstantBitRate(int network, int node, int timeBetweenSending) {
        super(network, node);
        _timeBetweenSending = timeBetweenSending;
    }

    /**
     * Returns the time between each packet. This will always be the same.
     *
     * @return Delay until the next packet should be sent.
     */
    @Override
    protected double getNextSendTime() {
        return _timeBetweenSending;
    }
}
