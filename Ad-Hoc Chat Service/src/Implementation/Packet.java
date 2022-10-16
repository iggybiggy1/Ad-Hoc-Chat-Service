package Implementation;

import java.nio.ByteBuffer;
import java.util.HashMap;


public class Packet {
    private int source;
    private int destination;

    /**
     * Setter for source address
     *
     * @param source address of source node (1-16)
     */
    public void setSrc(int source) {
        this.source = source;
    }

    /**
     * Getter for source address
     *
     * @return source address
     */
    public int getSrc() {
        return this.source;
    }

    /**
     * Setter for destination
     *
     * @param destination address of destination node (1-16)
     */
    public void setDest(int destination) {
        this.destination = destination;
    }

    /**
     * Getter for destination address
     *
     * @return destination address
     */
    public int getDest() {
        return this.destination;
    }

    /**
     * Constructor for Packet. Always includes source and destination.
     *
     * @param source      address of the source node (1-16)
     * @param destination address of the destination node (1-16)
     */
    public Packet(int source, int destination) {
        this.source = source;
        this.destination = destination;
    }

    /**
     * Method for creating a header for the large packet. Source and destination set with constructor.
     *
     * @param dataLen length of the payload
     * @param mde     flag for "more data expected"
     * @param seqnum  sequence number of the packet
     * @return header for 32-byte packet
     */
    public byte[] dataPcktHeader(int dataLen, int mde, int ttl, int seqnum, int rflag) {
        return new byte[]{(byte) Integer.parseUnsignedInt(String.format("%4s", Integer.toBinaryString(source)).replaceAll(" ", "0") + String.format("%4s", Integer.toBinaryString(destination)).replaceAll(" ", "0"), 2),
                (byte) Integer.parseUnsignedInt(String.format("%5s", Integer.toBinaryString(dataLen)).replaceAll(" ", "0") + String.format("%2s", Integer.toBinaryString(ttl)).replaceAll(" ", "0") + Integer.toBinaryString(mde).replaceAll(" ", "0"), 2),
                (byte) seqnum,
                (byte) rflag};
    }

    /**
     * Method for creating a header for the short packet (ACK). Source and destination set with constructor.
     *
     * @param ack acknowledgement number of the packet (sequence number + size of payload)
     * @return header for 2-byte packet (ACK)
     */
    public ByteBuffer shortPcktAck(int ack) {
        byte[] pkt = new byte[]{(byte) Integer.parseUnsignedInt(String.format("%4s", Integer.toBinaryString(source)).replaceAll(" ", "0") + String.format("%4s", Integer.toBinaryString(destination)).replaceAll(" ", "0"), 2)
                , (byte) Integer.parseUnsignedInt(String.format("%8s", "0" + Integer.toBinaryString(ack)).replaceAll(" ", "0"), 2)};
        return ByteBuffer.wrap(pkt);
    }

    /**
     * Method for creating a header for the short packet (ICMP). Source and destination set with constructor.
     *
     * @param reqFlag request forwarding table flag (0, when no request needed, 1 when request needed)
     * @param finFlag finish connection flag (0, when node stays in the network, 1 when node wants to leave the network)
     * @return header for 2-byte packet (ICMP)
     */
    public ByteBuffer shortPcktInfoICMP(int reqFlag, int finFlag, int flagAck) { // second bit is 0 if shortPacket is used for later addressing (not initial)
        String openFlag = "000";
        byte[] pkt = new byte[]{(byte) Integer.parseUnsignedInt(String.format("%4s", Integer.toBinaryString(source)).replaceAll(" ", "0") + String.format("%4s", Integer.toBinaryString(destination)).replaceAll(" ", "0"), 2)
                , (byte) Integer.parseUnsignedInt(String.format("%8s", "1" + "0" + Integer.toBinaryString(reqFlag) + Integer.toBinaryString(finFlag) + Integer.toBinaryString(flagAck) + openFlag).replaceAll(" ", "0"), 2)
        };
        return ByteBuffer.wrap(pkt);
    }

    /**
     * Method for creating a header for the short packet (ICMP, addressing done by a master node of the network). Source and destination set with constructor.
     * If two numbers are the same, repeat the process until each node has a distinct random number, from which an address can be derived.
     *
     * @return header for 2-byte packet (ICMP, initial addressing)
     */
    public ByteBuffer changedAddy() { // second bit is 1 if shortPacket is used for initial addressing
        byte[] pkt = new byte[]{(byte) Integer.parseUnsignedInt(String.format("%4s", Integer.toBinaryString(source)).replaceAll(" ", "0") + String.format("%4s", Integer.toBinaryString(destination)).replaceAll(" ", "0"), 2)
                , (byte) Integer.parseUnsignedInt(String.format("%8s", "1" + "1" + "1" + "00000").replaceAll(" ", "0"), 2)
        };
        return ByteBuffer.wrap(pkt);    // third bit might be used to determine whether the message received comes from a node that is a master node
    }                                   // 1 means master node, 0 means slave node

    /**
     * Method for creating a header for the short packet (ICMP, initial addressing). Source and destination set with constructor.
     * If two numbers are the same, repeat the process until each node has a distinct random number, from which an address can be derived.
     *
     * @return header for 2-byte packet (ICMP, initial addressing)
     */
    public ByteBuffer initialAddressing() { // second bit is 1 if shortPacket is used for initial addressing
        int rand = 00000;                               // parameter is not needed in this method (source is the random number)
        byte[] pkt = new byte[]{(byte) Integer.parseUnsignedInt(String.format("%4s", Integer.toBinaryString(source)).replaceAll(" ", "0") + String.format("%4s", Integer.toBinaryString(destination)).replaceAll(" ", "0"), 2)
                , (byte) Integer.parseUnsignedInt(String.format("%8s", "1" + "1" + "0" + "00000").replaceAll(" ", "0"), 2)
        };
        return ByteBuffer.wrap(pkt);    // third bit might be used to determine whether the message received comes from a node that is a master node
    }

    /**
     * Method for parsing the packet header with packet payload.
     *
     * @param pcktHeader actual header derived by aforementioned methods
     * @param data       actual payload that is to be delivered by the packet
     * @return actual packet (always 32-byte long, since short packets contain only the headers and no payload)
     */
    public ByteBuffer dataPckt(byte[] pcktHeader, byte[] data) {
        byte[] pkt = new byte[32];
        ByteBuffer buffer = ByteBuffer.wrap(pkt);
        buffer.put(pcktHeader);
        buffer.put(data);
        return buffer;
    }

    /**
     * Method for printing out the payload of a packet in textual form.
     *
     * @param bytes       data payload
     * @param bytesLength length of payload
     */
    public void printByteBuffer(ByteBuffer bytes, int bytesLength) {
        for (int i = 0; i < bytesLength; i++) {
            System.out.print((bytes.get(i)) + " ");
        }
        System.out.println();
    }

    /**
     * Converts a given HashMap into a ByteBuffer
     * @param lsl
     * @return buffer
     * */
    public ByteBuffer LinkStatePckt(HashMap<Integer, MyRoute> lsl) {
        byte[] pkt = new byte[28];
        ByteBuffer buffer = ByteBuffer.wrap(pkt);
        if (lsl.size() > 10) {
            System.out.println("\nLink State List too big for Link State Packet\n");
        }
        for (Integer key : lsl.keySet()) {
            byte[] lspkt = new byte[3];
            lspkt[0] = (byte) Integer.parseUnsignedInt(Integer.toBinaryString(key), 2);
            lspkt[1] = (byte) lsl.get(key).getCost();
            lspkt[2] = (byte) lsl.get(key).getNextHop();
            buffer.put(lspkt);
        }
        return buffer;
    }

}
