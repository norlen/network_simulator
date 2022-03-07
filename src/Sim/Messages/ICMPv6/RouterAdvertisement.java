package Sim.Messages.ICMPv6;

import Sim.NetworkAddr;

/**
 * Modified Router Advertisement for MobileIPv6.
 * <p>
 * The modified advertisement adds a home agent bit, signifying that the router acts as a home agent.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc4861#section-4.2
 * https://datatracker.ietf.org/doc/html/rfc6275#section-7.1
 */
public class RouterAdvertisement extends ICMPv6 {
    // ICMP type for Router Advertisement.
    public static final int ICMP_TYPE = 134;

    // ICMP code for Router Advertisement.
    public static final int ICMP_CODE = 0;

    public RouterAdvertisement(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, ICMP_TYPE, ICMP_CODE, seq);
    }
}
