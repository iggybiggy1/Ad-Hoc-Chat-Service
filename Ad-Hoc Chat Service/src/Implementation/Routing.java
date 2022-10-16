package Implementation;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Routing {
    private Addressing nodes;
    private HashMap<Integer, MyRoute> routingTable = new HashMap<>();
    private Map<Integer, Integer> forwardingTable = new HashMap<>();

    public Routing() {
    }

    //initiates the routing table according to the received addressing information, and adds first entry with own node and cost = 0
    public Routing(Addressing addressing) {
        nodes = addressing;
        MyRoute ownRoute = new MyRoute();
        //basically for 1st entry to link-state routing table
        ownRoute.setNextHop(nodes.getCurrentNode());
        ownRoute.setCost(0);
        routingTable.put(nodes.getCurrentNode(), ownRoute);
    }
    /**Adds a route to the routing table
     * @param - dest*/
    public void addRoute(int destination) {
        MyRoute newRoute = new MyRoute();
        newRoute.setCost(1);
        newRoute.setNextHop(destination);
        routingTable.put(destination, newRoute);
    }


    /***
     * extracts the routing information form a packet
     * @param bb
     * @return - LinkState[]
     */
    public LinkState[] extractRouting(ByteBuffer bb) {

        LinkState[] routing = new LinkState[10];
        for (int i = 4, j = 0; i < bb.capacity(); i = i + 3, j++) {
            if (bb.get(i) == 0) {
                break;
            }
            routing[j] = new LinkState();
            routing[j].setDestination(Byte.toUnsignedInt(bb.get(i)));
            routing[j].setCost(Byte.toUnsignedInt(bb.get(i + 1)));
            routing[j].setNextHop(Byte.toUnsignedInt(bb.get(i + 2)));
        }
        return routing;
    }

    public HashMap<Integer, MyRoute> getRoutingTable() {
        return this.routingTable;
    }
    //needs to receive packets with link-state information to form a forwarding table
    // these packets should have a field with link cost or have a different datatype completely

    /**
     * Updates the routing table using the latest received link-state information from packets
     * @param linkState
     */
    public void updateRouting(LinkState[] linkState) {
        int sourceNode = linkState[0].getDestination();
        for (LinkState entry : linkState) {
            if(entry == null){
                break;
            }
            if(entry.getCost() == 0){
                sourceNode = entry.getDestination();
            }
        }
        for (LinkState entry : linkState) {
            if(entry == null){
                break;
            }
            int destination = entry.getDestination();
            int cost = entry.getCost();
            //int nextHop = entry.getNextHop();
            if (routingTable.containsKey(destination)) {
                MyRoute oldRoute = routingTable.get(destination);
                if (cost + 1 < oldRoute.getCost()) {
                    MyRoute newRoute = new MyRoute();
                    newRoute.setCost(cost + 1);
                    newRoute.setNextHop(sourceNode);
                    routingTable.replace(destination, newRoute);
                }
            } else {
                MyRoute newRoute = new MyRoute();
                newRoute.setCost(cost + 1);
                newRoute.setNextHop(sourceNode);
                routingTable.put(destination, newRoute);
            }
        }
        updateForwarding();
    }

    /**
     * Updates the routing table according to the updates routing table
     */
    public void updateForwarding() {
        forwardingTable = new HashMap<>();
        for (Map.Entry<Integer, MyRoute> destNode : routingTable.entrySet()) {
            forwardingTable.put(destNode.getKey(), destNode.getValue().getNextHop());
        }
        forwardingTable.remove(nodes.getCurrentNode());
    }

    /**
     * Returns the current status of the nodes forwarding table
     */
    public Map<Integer, Integer> getForwardingTable() {
        return forwardingTable;
    }


    public void removeRoute(int dest) {
        routingTable.remove(dest);
    }
}