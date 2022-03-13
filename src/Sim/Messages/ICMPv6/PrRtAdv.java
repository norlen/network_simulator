package Sim.Messages.ICMPv6;

import Sim.NetworkAddr;

/**
 * Proxy Router Advertisement (PrRtAdv)
 * <p>
 * We send a simplified version of this, which is similar to regular advertisements. We also require that they be
 * solicited so flags for such things are also ignored.
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.1.2
 */
public class PrRtAdv extends RouterAdvertisement {
    // Name of the interface this advertisement came from, this is pretty much our link-local address.
    private final String _interfaceName;

    /**
     * Creates a new proxy router advertisement message.
     *
     * @param from          link-local address of sender.
     * @param to            source address of invoking router or the all nodes multicast address.
     * @param seq           sequence number.
     * @param name          next access router's name.
     * @param networkPrefix next access router's network prefix address, which all interface addresses are derived from.
     */
    public PrRtAdv(NetworkAddr from, NetworkAddr to, int seq, String name, long networkPrefix, String interfaceName) {
        super(from, to, seq, name, networkPrefix);
        _interfaceName = interfaceName;
    }

    /**
     * Returns the name of the interface this advertisement comes from.
     *
     * @return name of interface.
     */
    public String getInterfaceName() {
        return _interfaceName;
    }

    @Override
    public String toString() {
        return String.format("PrRtAdv (ICMPv6), src=%s, dst=%s", source(), destination());
    }
}
