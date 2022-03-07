package Sim.Messages.MobileIPv6;

import Sim.Messages.IPv6;
import Sim.NetworkAddr;

/**
 * Mobility Header provides an extended header used by mobile nodes.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc6275#section-6.1
 */
public abstract class MobilityHeader extends IPv6 {
    // This is not an exhaustive list of fields, see the reference for that. Only the used fields are present.

    // todo
    private final int _mhType;

    public MobilityHeader(NetworkAddr from, NetworkAddr to, int seq, int mhType) {
        super(from, to, seq);
        _mhType = mhType;
    }

    public int getMHType() {
        return _mhType;
    }
}
