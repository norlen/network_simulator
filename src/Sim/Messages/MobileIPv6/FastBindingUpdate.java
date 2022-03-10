package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Fast Binding Update (FBU)
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.2.2
 */
public class FastBindingUpdate extends MobilityHeader {
    public FastBindingUpdate(NetworkAddr from, NetworkAddr to, int seq, int mhType) {
        super(from, to, seq, mhType);
    }
}
