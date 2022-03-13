package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Fast Binding Acknowledgment (FBack)
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.2.3
 */
public class FastBindingAck extends BindingAck {
    public FastBindingAck(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq);
    }

    @Override
    public String toString() {
        return String.format("FastBindingAck (MH), src=%s, dst=%s", source(), destination());
    }
}
