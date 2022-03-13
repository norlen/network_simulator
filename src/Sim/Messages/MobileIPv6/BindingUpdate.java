package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Binding Update is used to notify other nodes that the node has a new care-of-address.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc6275#section-6.1.7
 */
public class BindingUpdate extends MobilityHeader {
    // Unsigned 16-bit. Use to sequence binding updates, will be returned in the Binding Acknowledgement.
    private final int _sequence;

    // When the MN is on a foreign network and sends binding updates we have to keep track of which one it is.
    private final NetworkAddr _homeAddress;

    public BindingUpdate(NetworkAddr from, NetworkAddr to, int seq, int sequence, NetworkAddr homeAddress) {
        super(from, to, seq);
        _sequence = sequence;
        _homeAddress = homeAddress;
    }

    public int getSequence() {
        return _sequence;
    }

    public NetworkAddr getHomeAddress() {
        return _homeAddress;
    }

    @Override
    public String toString() {
        return String.format("BindingUpdate (MH), src=%s, dst=%s", source(), destination());
    }
}
