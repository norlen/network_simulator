package Sim;

// This class represent a routing table entry by including
// the link connecting to an interface as well as the node 
// connected to the other side of the link
public class RouteTableEntry extends TableEntry {
    private final int _networkId;

    RouteTableEntry(int networkId, SimEnt link) {
        super(link, null);
        _networkId = networkId;
    }

    public int getNetworkId() {
        return _networkId;
    }

    public SimEnt link() {
        return super.link();
    }
}
