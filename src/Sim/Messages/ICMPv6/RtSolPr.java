package Sim.Messages.ICMPv6;

import Sim.NetworkAddr;

/**
 * Router Solicitation for Proxy Advertisement (RtSolPr)
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.1.1
 */
public class RtSolPr extends RouterSolicitation {
    // The RFC specifies that this should contain the new access point's link-layer address. However, we don't have
    // any addresses at all for routers. So we simplify this and take the name of the next access router instead.
    private final String _nar;

    /**
     * Instantiates a solicitation message.
     *
     * @param from IP address of interface or the unspecified address.
     * @param to   the all routers multicast address.
     * @param seq  sequence number.
     * @param nar  next access router name.
     */
    public RtSolPr(NetworkAddr from, NetworkAddr to, int seq, String nar) {
        super(from, to, seq);
        _nar = nar;
    }

    /**
     * Gets the name of the router to solicit an advertisement from.
     *
     * @return name of the next access router.
     */
    public String getNextAccessRouterName() {
        return _nar;
    }
}
