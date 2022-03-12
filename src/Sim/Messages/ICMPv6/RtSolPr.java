package Sim.Messages.ICMPv6;

import Sim.NetworkAddr;

/**
 * Router Solicitation for Proxy Advertisement (RtSolPr)
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.1.1
 */
public class RtSolPr extends RouterSolicitation {
    // The RFC specifies that this should contain the new access point's link-layer address. However, we don't have
    // any addresses at all for routers. So we simplify this and take what we have which is the name and the interface id.

    // Name of the next access router to solicit an advertisement from.
    private final String _name;

    // Specify which interface id, so we can get an address.
    private final int _interfaceId;

    /**
     * Instantiates a solicitation message.
     *
     * @param from        IP address of interface or the unspecified address.
     * @param to          the all routers multicast address.
     * @param seq         sequence number.
     * @param name        next access router name.
     * @param interfaceId next access router's interface id.
     */
    public RtSolPr(NetworkAddr from, NetworkAddr to, int seq, String name, int interfaceId) {
        super(from, to, seq);
        _name = name;
        _interfaceId = interfaceId;
    }

    /**
     * Gets the name of the router to solicit an advertisement from.
     *
     * @return name of the next access router.
     */
    public String getName() {
        return _name;
    }

    /**
     * Gets the interface id we want to get an advertisement from on the next access router.
     *
     * @return interface id of the next access router.
     */
    public int getInterfaceId() {
        return _interfaceId;
    }
}
