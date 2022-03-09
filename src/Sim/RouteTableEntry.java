package Sim;

// This class represent a routing table entry by including
// the link connecting to an interface as well as the node 
// connected to the other side of the link
public class RouteTableEntry extends TableEntry {
    private final long _prefix;
    private final int _bits;
    private final long _mask;
    private final int _interfaceId;

    RouteTableEntry(SimEnt link, long prefix, int numBits, int interfaceId) {
        super(link, null);
        _prefix = prefix;
        _bits = numBits;
        _mask = generateMask(numBits);
        _interfaceId = interfaceId;
    }

    public int getInterfaceId() {
        return _interfaceId;
    }

    public long getNetworkId() {
        return _prefix;
    }

    public boolean matches(long networkId) {
        return _prefix == (networkId & _mask);
    }

    /**
     * Generates the mask to mask out all the bits of the prefix part.
     *
     * @return mask with the first numBits bits set.
     */
    private long generateMask(int numBits) {
        long mask = 0;
        for (int i = 0; i < (64 - numBits); ++i) {
            mask <<= 1;
            mask |= 1;
        }
        return ~mask;
    }

    public int getNumBits() {
        return _bits;
    }
}
