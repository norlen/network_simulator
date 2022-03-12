package Sim;

// This class represent a routing table entry by including
// the link connecting to an interface as well as the node 
// connected to the other side of the link
public class RouteTableEntry extends TableEntry {
    private final NetworkAddr _addr;
    private final int _interfaceId;

    RouteTableEntry(SimEnt link, NetworkAddr addr, int interfaceId) {
        super(link, null);
        _addr = addr;
        _interfaceId = interfaceId;
    }

    public int getInterfaceId() {
        return _interfaceId;
    }

    public NetworkAddr getAddr() {
        return _addr;
    }
}
