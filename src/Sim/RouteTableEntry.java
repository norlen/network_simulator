package Sim;

// This class represent a routing table entry by including
// the link connecting to an interface as well as the node 
// connected to the other side of the link
public class RouteTableEntry extends TableEntry {
    private final int[] _networkIds;

    RouteTableEntry(int[] networkIds, SimEnt link) {
        super(link, null);
        _networkIds = networkIds;
    }

    public int[] getNetworkIds() {
        return _networkIds;
    }

    public SimEnt link() {
        return super.link();
    }
}
