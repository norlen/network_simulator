package Sim.Messages.ICMPv6;

import Sim.NetworkAddr;

/**
 * Router Solicitation for Proxy Advertisement (RtSolPr)
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.1.1
 */
public class RtSolPr extends ICMPv6 {
    // ICMP type for Router Solicitation.
    public static final int ICMP_TYPE = 154;

    // ICMP code for Router Solicitation.
    public static final int ICMP_CODE = 0;

    // Source Link-Layer address.
    private final NetworkAddr _srcAddr;

    // New Access Point Link-Layer Address.
    private final NetworkAddr _apAddr;

    /**
     * Creates a new ICMPv6 message.
     *
     * @param from source address.
     * @param to   destination address.
     * @param seq  sequence number.
     */
    public RtSolPr(NetworkAddr from, NetworkAddr to, int seq, NetworkAddr srcAddr, NetworkAddr apAddr) {
        super(from, to, seq, ICMP_TYPE, ICMP_CODE);
        _srcAddr = srcAddr;
        _apAddr = apAddr;
    }
}
