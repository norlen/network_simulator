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

    public NetworkAddr(long network, long node) {
        _networkId = network;
        _nodeId = node;
    }

    public long networkId() {
        return _networkId;
    }

    public long nodeId() {
        return _nodeId;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", getHex(_networkId), getHex(_nodeId));
    }

    private String getHex(long part) {
        return String.format("%h:%h:%h:%h", (part >> 48) & 0xFFFF, (part >> 32) & 0xFFFF, (part >> 16) & 0xFFFF, part & 0xFFFF);
    }
}
