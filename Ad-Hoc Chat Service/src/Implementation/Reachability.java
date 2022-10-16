package Implementation;

import client.Message;
import client.MessageType;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.concurrent.BlockingQueue;

public class Reachability implements Runnable{
    private SocketChannel sock;
    private BlockingQueue<Message> sendingQueue;
    private Addressing addressing;
    private ReliableTransfer stopAndWait;
    private boolean ACK = false;
    private ArrayList<Integer> activeNodes = new ArrayList();

    public Reachability(BlockingQueue<Message> sendingQueue, Addressing addressing, ReliableTransfer reliableTransfer){
        this.sendingQueue = sendingQueue;
        this.addressing = addressing;
        stopAndWait = reliableTransfer;
    }



    public void sendEcho(){
        //put a new echo package in the sending queue every 30seconds (we can change it still)
        try {
        Packet packet = new Packet(addressing.getCurrentNode(), 0);
        Message msg = new Message(MessageType.DATA_SHORT, packet.shortPcktInfoICMP(0,0,0));
            sendingQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        while (sock.isConnected()){
            try {
                Thread.sleep(15000);
                sendEcho();
                activeNodes.add(addressing.getCurrentNode());
                Thread.sleep(15000);
            }  catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
