package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Fast Binding Acknowledgment (FBack)
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.2.3
 */
public class FastBindingAck extends MobilityHeader {
    public FastBindingAck(NetworkAddr from, NetworkAddr to, int seq, int mhType) {
        super(from, to, seq, mhType);
    }
}
