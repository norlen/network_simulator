package Sim.Messages.ICMPv6;

import Sim.Messages.IPv6;
import Sim.NetworkAddr;

/**
 * Implementation of ICMPv6 messages.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc4443
 */
public abstract class ICMPv6 extends IPv6 {
    /**
     * Creates a new ICMPv6 message.
     *
     * @param from source address.
     * @param to   destination address.
     * @param seq  sequence number.
     */
    public ICMPv6(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq);
    }
}
