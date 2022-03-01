package Sim.Messages.MobileIPv6;

import Sim.Message;
import Sim.NetworkAddr;

/**
 * Mobility Header provides an extended header used by mobile nodes.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc6275#section-6.1
 */
public abstract class MobilityHeader extends Message {
    // There are header fields here, but none that we use right now. The class exist IF we want to use any of these.

    public MobilityHeader(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq);
    }
}
