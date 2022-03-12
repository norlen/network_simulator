package Sim.Messages.ICMPv6;

import Sim.NetworkAddr;

/**
 * Router Solicitation is a message sent to routers to make it generate Router Advertisements.
 * <p>
 * These are multicasted onto the network, and this implementation uses the unspecified address
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc4861#section-4.1
 */
public class RouterSolicitation extends ICMPv6 {
    /**
     * Instantiates a solicitation message.
     *
     * @param from IP address of interface or the unspecified address.
     * @param to   the all routers multicast address.
     * @param seq  sequence number.
     */
    public RouterSolicitation(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq);
    }
}
