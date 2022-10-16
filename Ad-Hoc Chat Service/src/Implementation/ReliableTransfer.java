package Implementation;

import client.Message;
import client.MessageType;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class ReliableTransfer {
   // boolean wait = false;
    private int timeOut = 1000;// 10x5 ms
    private ByteBuffer ack; // the ack received
    private ByteBuffer sentPkt; // the packet we send and wait ack for;
    private int seqNum=0;
    private int ackNum=0;
    private int dataLen=0;
    private int previousSeq = 0;
    private boolean received = false;
    private boolean timer = false;
    private String data = "";
    private int i = 0;
    private PriorityQueue<Message> oldPackets =  new PriorityQueue<>();
    // private BlockingQueue<Message> receivedQueue;
   // private BlockingQueue<Message> sendingQueue;
    private Addressing address;
    private Reachability reachability;
    private ArrayList<Integer> ackedNodes = new ArrayList<Integer>();
    private Routing routing;
    public ReliableTransfer(){

    }

    public ReliableTransfer(Addressing address){
        this.address = address;
        this.routing = new Routing(address);
    }
    /**
     * This method checks if the recieved message ack num is the right one in correspondance to the packet we sent
     *
     * @param msg a packet we recieve
     */
    public boolean ackReceived(Message msg) {       // sequence number not updated ???

        //start picking messages from the receivingQueue
        if (msg.getType() == MessageType.DATA_SHORT) {
            ack = msg.getData(); //we extract just the data
            ackNum = Byte.toUnsignedInt(ack.get(1)); // manipulating bits to drop the first bit // the first bit will be 0 iif its ack
           // dataLen = bitExtracted(Byte.toUnsignedInt(ack.get(1)), 5, 4);// AETIUASF
            ackNum = bitExtracted(Byte.toUnsignedInt(ack.get(1)),7,1);
//            ackNum = ackNum >>> 1;
            int flag = bitExtracted((ackNum), 1, 8);  // THE  SMALLCHANGES
            if (flag == 1){ // aTHAUSTBJA
                return true;
            }
                if ((seqNum + dataLen + 1) == ackNum || ackNum == 0) {//if acknowledgement number of receievd packet is equal to acknowledgement number of sent packet
                    int source = bitExtracted(Byte.toUnsignedInt(msg.getData().get(0)),4,5);
                    if(address.addNode(msg)){
                        routing.addRoute(source);
                    }
                    if(!ackedNodes.contains((Object) source)) {
                        ackedNodes.add(source);
                    }
                    ackAllNodes(ackNum);
//                    System.out.println("ACK RECEIVED\n");
                    return true;
             }

        }
        return false;
    }

    public void ackAllNodes(int ackNum){
        //ArrayList<Integer> addre = address.getNodes();
       // addre.remove(0);
        if (ackedNodes.size() == address.getNodes().size()-1){ // -1 because there we have ourselves
            System.out.println("\nALL ACK'S RECEIVED\n");
            received = true;
            seqNum = ackNum;
        }
    }

    /**
     * This method enters a timeout period and waits for an ack.
     * @return true if we get the right ack for the packet we sent in a specific amount of time
     */
    /**
     * updates the seqNum so next message would have  a  new, updated one
     * @param seqNum of the old message
     * @param dataLen of the message
     */
    public void updateSeqNum(int seqNum, int dataLen){
        this.seqNum = seqNum + dataLen + 1;
    }

    /**
     * This method enters a timeout period and waits for an ack.
     * @return true if we get the right ack for the packet we sent in a specific amount of time
     */
    public boolean enterTimeout(Message msg) {
        //need to think about what happens if we send an ack
        timeOut = 1000;
        if (msg.getType() == MessageType.DATA) {
            sentPkt = msg.getData();
            seqNum = Byte.toUnsignedInt(sentPkt.get(2));
            int data = Byte.toUnsignedInt(sentPkt.get(1));
            dataLen = bitExtracted(data, 5, 4);
        }
        while (timeOut != 0) {
            try {
                Thread.sleep(5);
                if(received) {
                    break;
                }
            } catch(InterruptedException e) {

            }
            timeOut--;
        }
        updateSeqNum(seqNum,dataLen);
        return received;
    }

    /**Adds a header on top of the given message with broadcast destination and the given flags
     * @param msg - message to be broadcasted
     * @param mde - more data expected - fragments flag
     * @param ttl - hops to live
     * @param rflag - routing flag
     * */
    //Creates a data packet for broadcast
    public void broadcastMessage(Message msg, int mde, int ttl, int rflag){
        Packet packet = new Packet(address.getCurrentNode(),0);
        byte[] header = packet.dataPcktHeader(msg.getData().array().length, mde, ttl, getSeqNum(), rflag);        //creates a header
        msg.setData(packet.dataPckt(header, msg.getData().array()));
    }
    /**Adds a header on top of the given message with broadcast destination and the given flags (for packets to be forwarded)
     * @param msg - message to be broadcasted
     * @param mde - more data expected - fragments flag
     * @param ttl - hops to live
     * @param rflag - routing flag
     * @param seqNum - given sequence Number
     * */
    public void broadcastMessage(int source, Message msg, int mde, int seqNum, int ttl, int rflag){
        Packet packet = new Packet(source,0);    // this should not be like this !!!
        byte[] header = packet.dataPcktHeader(msg.getData().array().length, mde, ttl, seqNum, rflag);        //creates a header
        msg.setData(packet.dataPckt(header, msg.getData().array()));
    }

    public int getSeqNum(){
        return seqNum;
    }

    /**Creates an ack for the received Bytebuffer
     * @param bb - received byteBuffer
     * @return - pack.shortPcktAck() - a short packet (an ACK)
     * */
    public ByteBuffer createAck (ByteBuffer bb){
        Packet pack = new Packet(address.getCurrentNode(), bitExtracted(Byte.toUnsignedInt(bb.get(0)),4,5));
        int source = bitExtracted(Byte.toUnsignedInt(bb.get(0)),4,5);
        Message message = new Message(MessageType.DATA_SHORT, bb);
        if(address.addNode(message)){
            routing.addRoute(source);
        }
        if(!ackedNodes.contains((Object) source)) {
            ackedNodes.add(source);
        }
        int seqNum = Byte.toUnsignedInt(bb.get(2)); // all of this is bit shifting for different purposes
        int dataLen = bitExtracted(bb.get(1),5,4);
        int ackNum = seqNum + dataLen + 1;
        return pack.shortPcktAck(ackNum);
    }

    /**checks if the receievd message is a new message
     * @param - msg - the receievd message
     * @return boolean*/
    private boolean newPacket(Message msg){
        for (Message m : oldPackets){
            if  (msg.compareTo(m)  ==  0){
                return false;
            }
        }
        if (oldPackets.size() >5){
            oldPackets.poll();
        }
        oldPackets.add(msg);
        return true;
    }

    /**creates an ack for addressing
     * @param - dest
     * @return - pack.shortPcktAck(0)
     * */
    private ByteBuffer createAddressingAck (int dest){
        Packet pack = new Packet(address.getCurrentNode(), dest);
        return pack.shortPcktAck(0);
    }

    /**Encloses the current routing table into a DATA packet and returns it*/
    public Message sendRouting(){
        Packet packet = new Packet(address.getCurrentNode(), 0);
        Message msg = new Message(MessageType.DATA, packet.LinkStatePckt(routing.getRoutingTable()));
        broadcastMessage(msg, 0 ,1, 1);
        return msg;
    }


    /**Sets the received object back to false*/
    public void resetReceived(){
        received = false;
    }

    /**Processes a received message and triggers further transmission accordingly
     * @param msg - freshly recieved message
     * @return msg - returns an ACK for a MessageType.DATA, or returns null for a MessageType.DATA_SHORT
     * */
    public Message processPacket(Message msg){
        ByteBuffer dataPacket = msg.getData();
        int destination = bitExtracted(Byte.toUnsignedInt(msg.getData().get(0)), 4, 1);
        int source = bitExtracted(Byte.toUnsignedInt(msg.getData().get(0)),4,5);
        if (destination == address.getCurrentNode() || destination == 0) {
            int flag;
            switch (msg.getType()) {
                case DATA_SHORT: {
                    flag = bitExtracted(Byte.toUnsignedInt(dataPacket.get(1)), 1, 8);
                    if (flag == 0) {
                        if (ackReceived(msg)) {
                            timer = true;
                        }
                    } else {
                        return handleICMP(msg);
                    }
                }
                break;
                case DATA: {
                    if(newPacket(msg)) {
                        byte[] packet = msg.getData().array(); // I DONT KNOW WHAT THIS ISSS PLEASE ADD COMMENTS AND EXPLAIN, AHHHHGHHHHHHHHH
                        int mde = bitExtracted(Byte.toUnsignedInt(packet[1]), 1, 1);
                        byte data2 = packet[1];
                        int data2ToInt = Byte.toUnsignedInt(data2);
                        int dataLen = bitExtracted(data2ToInt, 5, 4);
                        String currentData = new String(Arrays.copyOfRange(packet, 4, dataLen + 4));

                        int rflag = Byte.toUnsignedInt(packet[3]);
                        int sequenceNum = Byte.toUnsignedInt(packet[2]);
                        if (sequenceNum != previousSeq || sequenceNum == 0) {
                            if(rflag == 1){
                                System.out.print("\nROUTING INFO WAS RECEIVED\n");
                                int size = routing.getRoutingTable().size();
                                routing.updateRouting(routing.extractRouting(dataPacket)); //this is the packet with the header, the first 4 bytes contain the header
                                HashMap<Integer, MyRoute> routes = routing.getRoutingTable();
                                System.out.println("\tDest:\tCost:\tNextHop:");
                                for (Integer route: routes.keySet()){
                                    System.out.print(route);
                                    System.out.print("\t\t" + routes.get(route).getCost() + "\t\t");
                                    System.out.print(routes.get(route).getNextHop()+ "\n");
                                }
                                if(size < routing.getRoutingTable().size()){
                                    return sendRouting();
                                }
                                return null;
                            }
                            else {
                                if (mde == 1) {
                                    previousSeq = sequenceNum;
                                    data = data + currentData;
                                } else {
                                    previousSeq = sequenceNum;
                                    data = data + currentData;
                                    System.out.println("\n>User " + source + " has sent: " + data+"\n");
                                    resetData();

                                }
                            }
                        }
                        ByteBuffer ack = createAck(dataPacket);
                        msg = new Message(MessageType.DATA_SHORT, ack);
                        return msg;
                    }
                }
            }
        } else {
            ArrayList<Integer> test = address.getNodes();
            System.out.println("\nPACKET NOT FOR ME");
        }
    return null;
        }



    public void resetData(){
        data = "";
    }

    public int bitExtracted(int number, int k, int p)
    {
        return (((1 << k) - 1) & (number >> (p - 1)));
    }

    public void setReachability(Reachability reachability){
        this.reachability = reachability;
    }

    private Message handleICMP(Message msg){
        ByteBuffer data = msg.getData();
        int dest = bitExtracted(Byte.toUnsignedInt(msg.getData().get(0)), 4, 5);
        int src = bitExtracted(Byte.toUnsignedInt(msg.getData().get(0)), 4, 1);
        Packet packet = new Packet(address.getCurrentNode(), dest);
        int flag = Byte.toUnsignedInt(data.get(1)); // Flag 2 (address or echo)
        flag = bitExtracted(flag, 1, 7);
        if (flag == 0) {
            int finFlag = Byte.toUnsignedInt(data.get(1)); // Flag 2 (address or echo)
            finFlag = bitExtracted(finFlag, 1, 5);
            int reqFlag = Byte.toUnsignedInt(data.get(1)); // Flag 2 (address or echo)
            reqFlag = bitExtracted(reqFlag, 1, 4);
            int ackFlag = Byte.toUnsignedInt(data.get(1)); // Flag 2 (address or echo)
            ackFlag = bitExtracted(ackFlag, 1, 6);
            if (finFlag == 1){
                address.removeNode(dest);
                routing.removeRoute(dest);
            } if (reqFlag == 1){
                // ASK FOR FORWARDING TABLES: send the requesting node our forwarding table
            } if (ackFlag == 1){
                address.addNode(msg);

            } if (ackFlag == 0){
//                return sendResponseEcho(msg);
            }
            return null;
        } else {
            flag = Byte.toUnsignedInt(data.get(1)); // Flag 2 (address or echo)
            flag = bitExtracted(flag, 1, 6);
            if (flag == 1 && address.sameAddy(msg)){ // if flag for change your address is up
                address.removeNode(address.getCurrentNode());
                address.resetAddy();
                packet.setSrc(address.getCurrentNode());
                return new Message(MessageType.DATA_SHORT, packet.initialAddressing());
            } else if (flag == 0) {
                if (address.sameAddy(msg)) {
                    address.removeNode(dest);
                    return new Message(MessageType.DATA_SHORT, packet.changedAddy());
                    // this means that the message received has the same source as one of the other (already online) nodes.
                } else if (address.addNode(msg)){ // check if the address list DOES NOT contain the source of the new node
                    return new Message(MessageType.DATA_SHORT, createAddressingAck(dest));
                }
            }
        }
        return null;
    }
    public Message sendResponseEcho(Message msg){
        //put a new echo package in the sending queue every 30seconds (we can change it still)
            int dest = bitExtracted(Byte.toUnsignedInt(msg.getData().get(0)), 4,5);
            Packet packet = new Packet(address.getCurrentNode(), dest);
            Message m = new Message(MessageType.DATA_SHORT, packet.shortPcktInfoICMP(0,0,1));
            int test = Byte.toUnsignedInt(m.getData().get(1));
            return m;
    }


    public Routing getRouting() {
        return this.routing;
    }
}