package Sim;

// This class implements a node (host) it has an address, a peer that it communicates with
// and it count messages send and received.

public class Node extends SimEnt {
    protected NetworkAddr _id;
    protected SimEnt _peer;
    private int _sentmsg = 0;
    private int _seq = 0;

    // Estimated jitter calculated using the algorithm described in RFC 1889.
    private double _estimatedJitter = 0.0;

    // Difference in time between packet sent and current time for the last packet.
    private double _transit = 0.0;

    public Node(int network, int node) {
        super();
        _id = new NetworkAddr(network, node);
    }

    // Sets the peer to communicate with. This node is single homed
    public void setPeer(SimEnt peer) {
        _peer = peer;

        if (_peer instanceof Link) {
            ((Link) _peer).setConnector(this);
        }
    }

    public NetworkAddr getAddr() {
        return _id;
    }

    //**********************************************************************************
    // Just implemented to generate some traffic for demo.
    // In one of the labs you will create some traffic generators

    private int _stopSendingAfter = 0; //messages
    private int _timeBetweenSending = 10; //time between messages
    private int _toNetwork = 0;
    private int _toHost = 0;

    public void StartSending(int network, int node, int number, int timeInterval, int startSeq) {
        _stopSendingAfter = number;
        _timeBetweenSending = timeInterval;
        _toNetwork = network;
        _toHost = node;
        _seq = startSeq;
        send(this, new TimerEvent(), 0);
    }
    //**********************************************************************************

    // This method is called upon that an event destined for this node triggers.

    public void recv(SimEnt src, Event ev) {
        if (ev instanceof TimerEvent) {
            if (_stopSendingAfter > _sentmsg) {
                _sentmsg++;
                send(_peer, new Message(_id, new NetworkAddr(_toNetwork, _toHost), _seq), 0);
                send(this, new TimerEvent(), _timeBetweenSending);
                System.out.println("Node " + _id.networkId() + "." + _id.nodeId() + " sent message with seq: " + _seq + " at time " + SimEngine.getTime());
                _seq++;
            }
        }
        if (ev instanceof Message) {
            System.out.println("Node " + _id.networkId() + "." + _id.nodeId() + " receives message with seq: " + ((Message) ev).seq() + " at time " + SimEngine.getTime());

            // Run jitter estimation.
            // updateJitterEstimation(SimEngine.getTime(), ((Message) ev).getTimestamp());
            // System.out.println("Node estimated jitter: " + _estimatedJitter);
        }
    }

    /**
     * Updates the jitter estimation using algorithm described in RFC 1889, under section A.8 Estimating the
     * Interarrival Jitter (https://datatracker.ietf.org/doc/html/rfc1889#appendix-A.8).
     *
     * @param arrivalTime     the current time.
     * @param packetTimestamp timestamp of the incoming packet.
     */
    private void updateJitterEstimation(double arrivalTime, double packetTimestamp) {
        double transit = arrivalTime - packetTimestamp;
        double d = transit - _transit;
        _transit = transit;
        _estimatedJitter += (1.0 / 16.0) * (Math.abs(d) - _estimatedJitter);
    }
}
