package Sim.Messages.ICMPv6;

import Sim.NetworkAddr;

/**
 * Modified Router Advertisement for MobileIPv6.
 * <p>
 * All our solicitations are send from the unspecified address, so the destination for these are the all nodes multicast
 * address.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc4861#section-4.2
 * https://datatracker.ietf.org/doc/html/rfc6275#section-7.1
 */
public class RouterAdvertisement extends ICMPv6 {
    // Name of the router. Used as a sort of proxy for addressing.
    private final String _name;

    // Network prefix the router is on.
    private final long _networkPrefix;

    /**
     * Creates a new advertisement message.
     *
     * @param from          link-local address of sender.
     * @param to            source address of invoking router or the all nodes multicast address.
     * @param seq           sequence number.
     * @param name          router's name.
     * @param networkPrefix this router's network prefix address, which all interface addresses are derived from.
     */
    public RouterAdvertisement(NetworkAddr from, NetworkAddr to, int seq, String name, long networkPrefix) {
        super(from, to, seq);
        _name = name;
        _networkPrefix = networkPrefix;
    }

    /**
     * Get the network prefix.
     *
     * @return the network prefix.
     */
    public long getNetworkPrefix() {
        return _networkPrefix;
    }

    /**
     * Get the name of the router.
     *
     * @return name of router.
     */
    public String getName() {
        return _name;
    }

    @Override
    public String toString() {
        return String.format("RouterAdvertisement (ICMPv6), src=%s, dst=%s", source(), destination());
    }
}
