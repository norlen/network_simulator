package Sim;

// An example of how to build a topology and starting the simulation engine

import Sim.Traffic.ConstantBitRate;
import Sim.Traffic.Gaussian;
import Sim.Traffic.Poisson;
import Sim.Traffic.TrafficGenerator;

enum TrafficGeneratorType {
    NONE,
    CBR,
    GAUSSIAN,
    POISSON,
};

public class Run {
    public static void main(String[] args) {
        boolean useLossyLinks = true;
        TrafficGeneratorType trafficGeneratorType = TrafficGeneratorType.GAUSSIAN;

        // Creates two links.
        Link link1, link2;
        if (useLossyLinks) {
            // Use lossy links.
            link1 = new LossyLink(100, 50, 0.25);
            link2 = new LossyLink(100, 50, 0.25);
        } else {
            // Use regular links.
            link1 = new Link();
            link2 = new Link();
        }


        // Create two end hosts that will be communicating via the router
        Node host1, host2;
        if (trafficGeneratorType == TrafficGeneratorType.NONE) {
            host1 = new Node(1, 1);
            host2 = new Node(2, 1);
        } else if (trafficGeneratorType == TrafficGeneratorType.CBR) {
            host1 = new ConstantBitRate(1, 1, 10);
            host2 = new ConstantBitRate(2, 1, 10);
        } else if (trafficGeneratorType == TrafficGeneratorType.GAUSSIAN) {
            host1 = new Gaussian(1, 1);
            host2 = new Gaussian(2, 1);
        } else {
            host1 = new Poisson(1, 1);
            host2 = new Poisson(2, 1);
        }

        // Connect links to hosts
        host1.setPeer(link1);
        host2.setPeer(link2);

        // Creates as router and connect
        // links to it. Information about
        // the host connected to the other
        // side of the link is also provided
        // Note. A switch is created in same way using the Switch class
        Router routeNode = new Router(2);
        routeNode.connectInterface(0, link1, host1);
        routeNode.connectInterface(1, link2, host2);

        // Generate some traffic
        // host1 will send 3 messages with time interval 5 to network 2, node 1. Sequence starts with number 1
        // host2 will send 2 messages with time interval 10 to network 1, node 1. Sequence starts with number 10

        if (trafficGeneratorType == TrafficGeneratorType.NONE) {
            // host1.StartSending();
            // host2.StartSending(1, 1, 2, 10, 10);
        } else {
            var h1 = (TrafficGenerator) host1;
            var h2 = (TrafficGenerator) host2;
            h1.StartSending(new NetworkAddr(2, 2), 1000000, 1);
            h2.StartSending(new NetworkAddr(1, 1), 1000000, 1);
        }

        // Start the simulation engine and of we go!
        Thread t = new Thread(SimEngine.instance());

        t.start();
        try {
            t.join();
        } catch (Exception e) {
            System.out.println("The motor seems to have a problem, time for service?");
        }

        if (trafficGeneratorType != TrafficGeneratorType.NONE) {
            // Output results from sink.
            var h1 = (TrafficGenerator) host1;
            var h2 = (TrafficGenerator) host2;

            String host1Name = "host1_" + trafficGeneratorType;
            if (useLossyLinks) {
                host1Name += "_lossy";
            }
            String host2Name = "host2_" + trafficGeneratorType;
            if (useLossyLinks) {
                host2Name += "_lossy";
            }
            h1.getSink().saveResults(host1Name);
            h2.getSink().saveResults(host2Name);
        }
    }
}
