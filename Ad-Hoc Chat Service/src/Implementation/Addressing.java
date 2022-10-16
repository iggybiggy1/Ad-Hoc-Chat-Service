package Implementation;

import client.Message;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Addressing {
    private ArrayList<Integer> nodes = new ArrayList<>();
    private int currentNode = 0;
    private Packet packet;
    private int message;
    private ByteBuffer data;
    private boolean addressed;
    public Addressing() {
        currentNode = new Random().nextInt(15);
        while(currentNode == 0) {
            currentNode = new Random().nextInt(15);
        }
        nodes.add(currentNode);
    }

    /**
     * Method used for changing the address, whenever another node is already spotted with the same address
     */
    public void resetAddy(){
        int current = getCurrentNode();
        currentNode = new Random().nextInt(15);
        while(currentNode == 0) {
            currentNode = new Random().nextInt(15);
        }
        System.out.println("\nAddress changed from " + current + " to " + getCurrentNode() + "\n");
        if(nodes.size() == 0) {
            nodes.add(currentNode);
        } else {
            nodes.set(0, currentNode);
        }
    }

    /**
     * Extracts the k amount of bits from p position in the binary sequence
     * @param number binary number to be used
     * @param k number of bits to extract
     * @param p position where till where bit-shiftting should happen
     * @return integer value extracted from that binary sequence
     */

    public int bitExtracted(int number, int k, int p)
    {
        return (((1 << k) - 1) & (number >> (p - 1)));
    }

    public boolean sameAddy(Message msg){
        int src = bitExtracted(Byte.toUnsignedInt(msg.getData().get(0)),4,5);
        if (src == currentNode){
            return true;
        }
        return false;
    }

    /**
     * Method used for adding the node address to addressing list of current node
     * @param msg received from the network
     * @return boolean that determines whether the node was already in addressing table or not
     */
    public boolean addNode(Message msg){

        byte src = msg.getData().get(0);
        int ass = Byte.toUnsignedInt(src);
        int finalAss = bitExtracted(ass, 4,5);
        if (nodes.contains(finalAss)){
            return false;
        }
        nodes.add(finalAss);
        return true;
    }

    /**
     * return true if this node has undergone the addressing process and has an address
     * @return the boolean for this
     */
    public boolean getAddressed(){
        return addressed;
    }

    /**removes the given node from the address list
     *
     * */
    public void removeNode(int addy){
        Object object = (Object) addy;
        nodes.remove(object);
    }

    public int getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(int address) {
        this.currentNode = address;
    }

    public ArrayList<Integer> getNodes(){
        return nodes;
    }

}
