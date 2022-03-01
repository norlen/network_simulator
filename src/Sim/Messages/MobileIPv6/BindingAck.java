package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Binding Acknowledgement
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc6275#section-6.1.8
 */
public class BindingAck extends MobilityHeader {
    public BindingAck(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq);
    }
}
