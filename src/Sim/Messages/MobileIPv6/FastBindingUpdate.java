package Sim.Messages.MobileIPv6;

import Sim.NetworkAddr;

/**
 * Fast Binding Update (FBU)
 * <p>
 * Reference: https://datatracker.ietf.org/doc/html/rfc5568#section-6.2.2
 */
public class FastBindingUpdate extends BindingUpdate {
    // The new care of address, which is in the NAR network.
    private final NetworkAddr _newCareOfAddress;

    // Next router's interface name.
    private final String _interfaceName;

    /**
     * Create a FastBindingUpdate message. These can in reality be sent both from the previous network and the new
     * network. In our implementation we only support sending these from the previous network.
     *
     * @param from             the current care of address.
     * @param to               the current router.
     * @param seq              sequence number.
     * @param newCareOfAddress the new care of address in the next router network.
     */
    public FastBindingUpdate(NetworkAddr from, NetworkAddr to, int seq, int sequence, NetworkAddr homeAddress, String interfaceName, NetworkAddr newCareOfAddress) {
        super(from, to, seq, sequence, homeAddress);
        _interfaceName = interfaceName;
        _newCareOfAddress = newCareOfAddress;
    }

    /**
     * Gets the new care of address.
     *
     * @return new care of address.
     */
    public NetworkAddr getNewCareOfAddress() {
        return _newCareOfAddress;
    }

    public String getInterfaceName() {
        return _interfaceName;
    }

    @Override
    public String toString() {
        return String.format("FastBindingUpdate (MH), src=%s, dst=%s", source(), destination());
    }
}
