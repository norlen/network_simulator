package Sim;

// An example of how to build a topology and starting the simulation engine

import Sim.Traffic.*;

import java.util.Date;

enum TrafficGeneratorType {
    NONE,
    CBR,
    GAUSSIAN,
    POISSON,
}

public class Run {
    public static void main(String[] args) {
        boolean useLossyLinks = false;
        TrafficGeneratorType trafficGeneratorType = TrafficGeneratorType.POISSON;
        int packetsToSend = 1_000_000;
        int lossDelay = 100;
        int lossJitter = 5;
        double lossDrop = 0.05;
        int arg = 1;

        // Creates two links.
        Link link1, link2;
        if (useLossyLinks) {
            // Use lossy links.
            link1 = new LossyLink(lossDelay, lossJitter, lossDrop);
            link2 = new LossyLink(lossDelay, lossJitter, lossDrop);
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
            host1 = new ConstantBitRate(1, 1, arg);
            host2 = new TrafficSink(2, 1);
        } else if (trafficGeneratorType == TrafficGeneratorType.GAUSSIAN) {
            host1 = new Gaussian(1, 1, arg, 15);
            host2 = new TrafficSink(2, 1);
        } else {
            host1 = new Poisson(1, 1, arg);
            host2 = new TrafficSink(2, 1);
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
            host1.StartSending(2, 2, 10, 5, 1);
            host2.StartSending(1, 1, 2, 10, 10);
        } else {
            var h1 = (TrafficGenerator) host1;
            h1.StartSending(new NetworkAddr(2, 2), packetsToSend, 1);
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
            var h2 = (TrafficSink) host2;
            String filename = trafficGeneratorType.toString() + "_" + arg;
            if (useLossyLinks) {
                filename += "_lossy_d" + lossDelay + "_j" + lossJitter + "_p" + lossDrop;
                int numDropped = ((LossyLink) link1).getNumDroppedPackets() + ((LossyLink) link2).getNumDroppedPackets();
                filename += "_dropped_" + numDropped;
            }
            String suffix = "";
            while (packetsToSend % 1000 == 0) {
                if (suffix.equals("")) {
                    suffix = "k";
                } else if (suffix.equals("k")) {
                    suffix = "m";
                } else {
                    break;
                }
                packetsToSend /= 1000;
            }
            filename += "_pkts_" + packetsToSend + suffix;
            filename += "_" + (new Date()).getTime();
            filename += ".csv";
            h2.saveTimeBetweenPackages(filename);
        }
    }
}
