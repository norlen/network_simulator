package Sim.Messages.ICMPv6;

import Sim.NetworkAddr;

/**
 * Proxy Router Advertisement (PrRtAdv)
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.1.2
 */
public class PrRtAdv extends ICMPv6 {
    // ICMP type for Router Solicitation.
    public static final int ICMP_TYPE = 154;

    // ICMP code for Router Solicitation.
    public static final int ICMP_CODE = 0;


    /**
     * Creates a new ICMPv6 message.
     *
     * @param from source address.
     * @param to   destination address.
     * @param seq
     */
    public PrRtAdv(NetworkAddr from, NetworkAddr to, int seq) {
        super(from, to, seq, ICMP_TYPE, ICMP_CODE);
    }
}
