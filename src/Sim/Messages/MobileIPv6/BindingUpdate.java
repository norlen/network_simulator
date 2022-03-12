package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Binding Update is used to notify other nodes that the node has a new care-of-address.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc6275#section-6.1.7
 */
public class BindingUpdate extends MobilityHeader {
    // Set to request a Binding Acknowledgement.
    private final boolean _acknowledge = false;

    // Set to make the receiving node act as the home agent.
    private final boolean _homeRegistration = false;

    // Unsigned 16-bit. Use to sequence binding updates, will be returned in the Binding Acknowledgement.
    private final int _sequence = 0;

    // Unsigned 16-bit. Lifetime of the binding until it must be considered expired. One time unit is 4 seconds.
    private final int _lifetime = 0;

    public BindingUpdate(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq);
    }

    public int getSequence() {
        return _sequence;
    }
}
