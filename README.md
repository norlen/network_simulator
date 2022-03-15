# Network Simulator

Lab material for D0021E.

## Lossy link

The regular link has been extended to create a `Lossy Link` the lossy link has options for introducing delay, jitter and
packet loss.

## Traffic Generators & Sinks

Three different types of traffic generators have been added. These include Constant Bit Rate, Gaussian distribution, and
Poisson distribution.

There are also different sinks, which can keep track of different stats, and some write their results to a file.

## Mobility

Nodes can move between routers and interfaces. Disconnecting from their current network and enter new networks.

`Router Solicitation` and `Router Advertisement` from the Neighbor Discovery protocol have been implemented. In
addition, the router has been extended so it can act as a Home Agent. A Home Agent accepts `Binding Update` to set a new
care of address for mobile nodes. When a binding has been updated a Home Agent responds with a `Binding Ack`

To reduce the time when a node cannot be reached when switching networks support for Mobile IPv6 Fast Handovers is
implemented. This addition include the messages `RtSolPr` and `PrRtAdv` for solicitation and advertisement of other
networks. `Fast Binding Update` and `Fast Binding Ack`, as well as `Handover Initiate` and `Handover Acknowledge` to
facilitate the handover procedure from the current router to the next.
