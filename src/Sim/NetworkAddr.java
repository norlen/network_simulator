package Sim;

// This class represent the network address, it consists of a network identity
// "_networkId" represented as an integer (if you want to link this to IP number it can be
// compared to the network part of the IP address like 132.17.9.0). Then _nodeId represent
// the host part.

/**
 * IPv6 address.
 */
public class NetworkAddr {
    public static final NetworkAddr UNSPECIFIED = new NetworkAddr(0x0L, 0x0L);

    public static final NetworkAddr ALL_NODES_MULTICAST = new NetworkAddr(0xFF02_0000_0000_0000L, 0x0000_0000_0000_0001);
    public static final NetworkAddr ALL_ROUTER_MULTICAST = new NetworkAddr(0xFF02_0000_0000_0000L, 0x0000_0000_0000_0002);

    private long _networkId;
    private long _nodeId;
    private int _prefix;
    private long _mask;

    public NetworkAddr(long network, long node) {
        _networkId = network;
        _nodeId = node;
        _prefix = 64;
        _mask = createMask(_prefix);
    }

    public NetworkAddr(long network, long node, int prefix) {
        _networkId = network;
        _nodeId = node;
        _prefix = prefix;
        _mask = createMask(_prefix);
    }

    public long networkId() {
        return _networkId;
    }

    public long nodeId() {
        return _nodeId;
    }

    public boolean matches(long networkId) {
        return (_networkId & _mask) == (networkId & _mask);
    }

    public int getPrefixBits() {
        return _prefix;
    }

    @Override
    public String toString() {
        return String.format("%s:%s/%d", getHex(_networkId), getHex(_nodeId), _prefix);
    }

    private String getHex(long part) {
        return String.format("%h:%h:%h:%h", (part >> 48) & 0xFFFF, (part >> 32) & 0xFFFF, (part >> 16) & 0xFFFF, part & 0xFFFF);
    }

    /**
     * Generates the mask to mask out all the bits of the prefix part.
     *
     * @return mask with the first numBits bits set.
     */
    private long createMask(int numBits) {
        long mask = 0;
        for (int i = 0; i < (64 - numBits); ++i) {
            mask <<= 1;
            mask |= 1;
        }
        return ~mask;
    }
}
