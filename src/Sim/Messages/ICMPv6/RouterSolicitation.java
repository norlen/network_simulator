package Sim.Messages.ICMPv6;

import Sim.NetworkAddr;

/**
 * Router Solicitation is a message sent to routers to make it generate Router Advertisements.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc4861#section-4.1
 */
public class RouterSolicitation extends ICMPv6 {

    public RouterSolicitation(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq, 133, 0);
    }
}
