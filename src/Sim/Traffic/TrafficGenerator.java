package Sim.Traffic;

import Sim.*;

/*
    Does the Traffic Geneator has to be a SimEnt? Or should it instead be composable,
    I.e. should they be passed to e.g. a Node in StartSending, so traffic can be generated
    over a certain link.

    And then only expose a function such as `CreateMessage`, although, whne should it be called again?
 */

public class TrafficGenerator extends SimEnt {
    private int _seq = 0;

    private Node _host;
    private NetworkAddr _dst;

    private int _messagesSent = 0;

    public TrafficGenerator(Node host) {
        _host = host;
    }

    public void StartSending(NetworkAddr dst) {
        _dst = dst;
        send(this, new TimerEvent(), 0);
    }

    @Override
    public void recv(SimEnt source, Event event) {
        if (event instanceof TimerEvent) {
            if (_messagesSent > 10) return;

            var src = _host.getAddr();
            var msg = new SendMessageEvent(_dst, 0);
            send(_host, msg, 0);

            _seq += 1;
            _messagesSent += 1;
            send(this, new TimerEvent(), 1.0);
        }
    }
}
