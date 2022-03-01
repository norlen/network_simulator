package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Binding Update is used to notify other nodes that the node has a new care-of-address.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc6275#section-6.1
 */
public class BindingUpdate extends MobilityHeader {
    public static final int MHType = 5;

    // The fields are not exhaustive, check reference for all possible fields. The fields here are the ones that are
    // required for our implementation.

    // Set to request a Binding Acknowledgement.
    private final boolean _acknowledge = false;

    // Set to make the receiving node act as the home agent.
    private final boolean _homeRegistration = false;

    // Unsigned 16-bit. Use to sequence binding updates, will be returned in the Binding Acknowledgement.
    private final int _sequence = 0;

    // Unsigned 16-bit. Lifetime of the binding until it must be considered expired. One time unit is 4 seconds.
    private final int _lifetime = 0;

    //private final NetworkAddr _newAddr;

    public BindingUpdate(NetworkAddr from, NetworkAddr to, int seq, NetworkAddr newAddr) {
        super(from, to, seq, MHType);
        //_newAddr = newAddr;
    }

//    public NetworkAddr getNewAddr() {
//        return _newAddr;
//    }
}
