package Sim;

// An example of how to build a topology and starting the simulation engine

import Sim.Events.StartHandover;
import Sim.Traffic.ConstantBitRate;
import Sim.Traffic.CountingSink;
import Sim.Traffic.Sink;
import Sim.Traffic.TrafficGenerator;

public class Run {
    public static void main(String[] args) {
        // Creates two links.
        Link link1, link2;
        if (true) {
            int linkDelay = 100;
            int linkJitter = 0;
            double linkPacketDropRate = 0;

            // Use lossy links.
            link1 = new LossyLink(linkDelay, linkJitter, linkPacketDropRate);
            link2 = new LossyLink(linkDelay, linkJitter, linkPacketDropRate);
        } else {
            // Use regular links.
            link1 = new Link();
            link2 = new Link();
        }

        // Network addresses.
        var HNAddress = new NetworkAddr(0x1111_0000_0000_0000L, 0, 32);
        var FNAddress = new NetworkAddr(0x2222_0000_0000_0000L, 0, 32);

        // Node addresses. Network portion is the interface's address.
        var MNAddress = new NetworkAddr(0x1111_0000_0000_0000L, 1);
        var CNAddress = new NetworkAddr(0x2222_0000_0000_0000L, 2);


        // Create nodes.
        TrafficGenerator host1_traffic = new ConstantBitRate(0, 100);
        TrafficGenerator host2_traffic = new ConstantBitRate(20, 100);
        Sink host1_sink = new CountingSink();
        Sink host2_sink = new CountingSink();

        Node host1 = new Node("MN", MNAddress, HNAddress, host1_traffic, host1_sink);
        Node host2 = new Node("CN", CNAddress, FNAddress, host2_traffic, host2_sink);

        // Connect links to hosts
        host1.setPeer(link1);
        host2.setPeer(link2);

        // Creates as router and connect links to it. Information about the host connected to the other side of the link
        // is also provided.
        Router routeNode = new Router("HA", 5, HNAddress);
        Router routeNode2 = new Router("R2", 5, FNAddress);
        routeNode.connectInterface(0, MNAddress, link1);
        routeNode2.connectInterface(0, CNAddress, link2);

        // Create a connection between routers.
        Link routerToRouter = new Link();
        routeNode.connectInterface(1, FNAddress, routerToRouter);
        routeNode2.connectInterface(1, HNAddress, routerToRouter);

        boolean fastHandover = true;
        StartHandover handover;
        if (!fastHandover) {
            handover = new StartHandover(routeNode2, 3); // Regular handover.
        } else {
            handover = new StartHandover(routeNode2, "R2", 3); // Fast handover.
        }
        SimEngine.instance().register(host1, host1, handover, 1000);

        routeNode.startSendingProxyAdvertisements(0);
        routeNode2.startSendingProxyAdvertisements(0);

        //SimEngine.instance().register(link1, routeNode, new EnterNetwork(host1, 0), 0);
        //SimEngine.instance().register(link2, routeNode2, new EnterNetwork(host2, 0), 0);

        // Generate some traffic
        host1.StartSending(CNAddress, 0);
        host2.StartSending(MNAddress, 0);

        // Start the simulation engine and off we go!
        Thread t = new Thread(SimEngine.instance());
        t.start();
        try {
            t.join();
        } catch (Exception e) {
            System.out.println("The motor seems to have a problem, time for service?");
        }

        host1.onSimulationComplete();
        host2.onSimulationComplete();
        routeNode.onSimulationComplete();
        routeNode2.onSimulationComplete();

//        if (false && trafficGeneratorType != TrafficGeneratorType.NONE) {
//            // Output results from sink.
//            var h2 = (FileSink) host2.getSink();
//            String filename = trafficGeneratorType.toString();
//            if (useLossyLinks) {
//                filename += "_lossy_d" + lossDelay + "_j" + lossJitter + "_p" + lossDrop;
//                int numDropped = ((LossyLink) link1).getNumDroppedPackets() + ((LossyLink) link2).getNumDroppedPackets();
//                filename += "_dropped_" + numDropped;
//            }
//            String suffix = "";
//            while (packetsToSend % 1000 == 0) {
//                if (suffix.equals("")) {
//                    suffix = "k";
//                } else if (suffix.equals("k")) {
//                    suffix = "m";
//                } else {
//                    break;
//                }
//                packetsToSend /= 1000;
//            }
//            filename += "_pkts_" + packetsToSend + suffix;
//            filename += "_" + (new Date()).getTime();
//            filename += ".csv";
//            h2.saveTimeBetweenPackages(filename);
//        }
    }
}
