import Implementation.*;
import client.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
* This is just some example code to show you how to interact 
* with the server using the provided 'Client' class and two queues.
* Feel free to modify this code in any way you like!
*/

public class MyProtocol{

    // The host to connect to. Set this to localhost when using the audio interface tool.
    private static String SERVER_IP = "netsys.ewi.utwente.nl"; //"127.0.0.1";
    // The port to connect to. 8954 for the simulation server.
    private static int SERVER_PORT = 8954;
    // The frequency to use.
    private static int frequency = 6400;
    private MAC mac;
    private Addressing addressing = new Addressing();   // initial addressing when no nodes are connected
    private ReliableTransfer stopAndWait = new ReliableTransfer(addressing);
    private Message ack;
    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;
    boolean free = false;
    private ArrayList<byte[]> fragmented_message;
    private Reachability reachable;
    private Routing routing;

    public MyProtocol(String server_ip, int server_port, int frequency){
        receivedQueue = new LinkedBlockingQueue<Message>();
        sendingQueue = new LinkedBlockingQueue<Message>();

        reachable = new Reachability(sendingQueue, addressing, stopAndWait);

        Client client = new Client(SERVER_IP, SERVER_PORT, frequency, receivedQueue, sendingQueue, stopAndWait, addressing ,reachable); // Give the client the Queues to use
        new receiveThread(receivedQueue).start(); // Start thread to handle received messages!
        printMenu();
        Scanner scanner = new Scanner(System.in);
       // sending("hi");
        String input = "";
        while(true){
            printEnterInput();
            input = scanner.nextLine(); // read input
            String[] commands = input.split(" ");

            if (input.startsWith("-")){
                String command;
                if (input.contains(">")){
                    command = input.toLowerCase().substring(input.indexOf("-") + 1, input.indexOf(">"));
                }else {
                    try {
                        command = input.toLowerCase().substring(input.indexOf("-") + 1, input.indexOf(" "));
                    } catch (StringIndexOutOfBoundsException e) {
                        command = input.toLowerCase().substring(input.indexOf("-") + 1, input.length());
                    }
                }
                switch (command){
                    case "help": case "h":
                        printMenu();
                        break;
                    case "quit": case "q":
                        Message msg;
                        Packet pkt = new Packet(addressing.getCurrentNode(),0);
                        msg = new Message(MessageType.DATA_SHORT, pkt.shortPcktInfoICMP(0,1,0));
                        try {
                            sendingQueue.put(msg);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        addressing.removeNode(addressing.getCurrentNode());
                        System.exit(0);
                        break;
                    case "list": case "l": case "userlist": case "users": case "u":
                        printList(addressing.getNodes());
                        break;
                    case "message": case "m": case "send": case "s": case "M":
                        String input1 = input.replace("-" + command + " ", "");
                        sending(input1);
                        break;
                    case "messageto": case "sendto": case "mt": case "st":
                        String replace;
                        String target;
                        String input2;
                        try {
                            target = input.substring(input.indexOf(">"), input.indexOf(" "));
                        }catch (StringIndexOutOfBoundsException e){
                            target = input.substring(input.indexOf(">"), input.length());
                        }
                        System.out.println(target);
                        try {
                            int startIndex = input.indexOf("-");
                            int endIndex = input.indexOf(" ");
                            replace = input.substring(startIndex, endIndex+1);
                            input2 = input.replace(replace, "");
                        }catch (StringIndexOutOfBoundsException e){
                            int startIndex = input.indexOf("-");
                            replace = input.substring(startIndex, input.length());
                            input2 = input.replace(replace, "");
                        }
                        System.out.println(replace);
                        System.out.println(input2);
                        Packet packet = new Packet(addressing.getCurrentNode(), Integer.parseInt(target));
                        break;
                    case "about": case "a":
                        printProjectInfo();
                        break;
                    case "request": case "r": case "requestforwardingtable": case "rft": case "forwardingtable": case "ft":
                        printRequestFT();
                        break;
                    default:
                        System.out.println("Command wasn't clear. For a list of possible commands, please use -Help.");
                        break;
                }
            }else{
                System.out.println("To use our application, please enter applicable commands. For a list of possible commands, please use -Help.");
            }

        }
    }

    /**Method for making the input of the user into a packet with a proper header and sending it out.
     *
     * @param input from the user
     */
    public void sending(String input){
        Message msg;
        byte[] inputBytes = input.getBytes(); // get bytes from input
        ByteBuffer toSend = ByteBuffer.allocate(inputBytes.length); // make a new byte buffer with the length of the input string
        toSend.put(inputBytes, 0, inputBytes.length); // copy the input string into the byte buffer.
        fragmentPacket(inputBytes);
        int hopsToLive = 1;
        routing = stopAndWait.getRouting();
        for(int j : routing.getRoutingTable().keySet()){
            int currentCost = routing.getRoutingTable().get(j).getCost();
            if (hopsToLive < currentCost) {
                hopsToLive = currentCost;
            }
        }
        if(input.length() > 28){      //if yes packet should be fragmented
            for(int i = 0;i < fragmented_message.size(); i++) {
                try{
                    Thread.sleep(7000);
                } catch(InterruptedException e) {

                }
                byte[] fragment = fragmented_message.get(i);
                ByteBuffer fragBuffer = ByteBuffer.allocate(fragment.length);
                fragBuffer.put(fragment, 0 , fragment.length);
                msg = new Message(MessageType.DATA, fragBuffer);
                int flag;
                if(i == fragmented_message.size()-1){
                    flag = 0;
                } else {
                    flag = 1;
                }
                stopAndWait.broadcastMessage(msg, flag, hopsToLive, 0);
                try {
                    sendingQueue.put(msg);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            msg = new Message(MessageType.DATA, toSend);
            stopAndWait.broadcastMessage(msg, 0, hopsToLive, 0);
            try {

                sendingQueue.put(msg);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void printRequestFT(){
        System.out.println("\n ~~ Requesting Forwarding Table ~~ ");
        System.out.println("");
    }

    public void printProjectInfo(){
        System.out.println("\n ~~ Project Information ~~");
        System.out.println("Ad-Hoc chat service: ScubaChat - Integration project - Group 59\nMade in: 04/2022\nMade by:" +
                "Ignacy Kepka, " +
                "Tristan van Beurden, " +
                "Kipras Klimkevicius and " +
                "Ujjwal Dodeja\n");
    }

    public void printList(ArrayList<Integer> nodes){
        System.out.println("\n ~~ User List ~~");
        for (int i = 0; i < nodes.size(); i++) {
            System.out.println(String.format(">User %s is available",nodes.get(i)));
        }
    }

    public void printMenu(){
        System.out.println("\n\n ~~ MENU ~~");
        System.out.println(String.format("Welcome User %s. If you want to use a command, please start with '-' and don't include spaces. \nCommands you may use:" +
                "\n-Help\t-h" +
                "\n-Message\t-m" +
                "\n-UserList\t-u OR -l" +
                "\n-About" +
                "\n-Quit",addressing.getCurrentNode()));
    }

    public void printEnterInput(){
        System.out.print(String.format("\n>User %s: ",addressing.getCurrentNode()));
    }
/**
*   sends data to be sent every time MAC exponentialBackoff returns true, also increments MACs resendCounter which is important to implement
 *   exponential backoff
 */

    private void setTimedOut(boolean ackReceived){
      boolean timedOut = ackReceived;
    }

    /**Method for extracting a certain amount of bits from a byte in the form of an unsigned integer
     * @param number the byte in an unsigned int value
     * @param k the amount of bits to extract counting from left to right
     * @param p the position of the bit you want to extract first. (position one is the last position
     *          when reading from left to right)
     * @return the int value of the bits extracted
     */
    public int bitExtracted(int number, int k, int p)
    {
        return (((1 << k) - 1) & (number >> (p - 1)));
    }

    /**Initiates the routing sequence*/
    public void initiateRouting(){
        routing = new Routing(addressing);
        Packet packet = new Packet(addressing.getCurrentNode(), 0); //send to all or specific
        Message msg = new Message(MessageType.DATA, packet.LinkStatePckt(routing.getRoutingTable()));
        // System.out.print("\n"+routing.getRoutingTable() );
        stopAndWait.broadcastMessage(msg, 0, 1, 1);
        try {
            sendingQueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /***
     * fragments an input string
     * @param inputBytes
     */
    public void fragmentPacket(byte[] inputBytes) {
        // round up to the number of packets it will need
        fragmented_message = new ArrayList<>();
        int size = inputBytes.length/28;
        if(size%28 != 0){
            size++;
        }
        for (int i = 0; i < size; i++) {
            //if it selects more than is available it simply adds 0x00, hence this is suitable even if the input data is less than 28 bytes
            if (i == size-1) {
                fragmented_message.add(Arrays.copyOfRange(inputBytes, i*28, inputBytes.length));
            } else {
                fragmented_message.add(Arrays.copyOfRange(inputBytes, i * 28, (i + 1) * 28));
            }
        }
    }



    public static void main(String args[]) {
        if(args.length > 0){
            frequency = Integer.parseInt(args[0]);
        }
        new MyProtocol(SERVER_IP, SERVER_PORT, frequency);        
    }


    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;

        public receiveThread(BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
        }
/**Method for sending initial addressing information
 *
  */
        private void addressing() {
            Message message;
            Packet packet = new Packet(addressing.getCurrentNode(), 0);
            message = new Message(MessageType.DATA_SHORT, packet.initialAddressing());
            try {
                sendingQueue.put(message);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /***
         * creates a packet to be forwarded
         * @param msg
         */
        public void forwardPacket(Message msg) {
            ByteBuffer dataReceived = msg.getData();
            int source = bitExtracted(Byte.toUnsignedInt(msg.getData().get(0)),4,5);
            int byte2 = Byte.toUnsignedInt(dataReceived.get(1));
            int mde = bitExtracted(byte2, 1, 1);
            int hopsToLive = bitExtracted(byte2, 2, 2);
            int sequenceNum = Byte.toUnsignedInt(dataReceived.get(2));
            byte[] bytes = dataReceived.array();
            int dataLen = bitExtracted(byte2, 5, 4);
            byte[] currentData = Arrays.copyOfRange(bytes, 4, dataLen + 4);
            ByteBuffer dataToSend = ByteBuffer.allocate(currentData.length);
            dataToSend.put(currentData, 0, currentData.length);
            if(hopsToLive > 1){
                Message newMsg = new Message(msg.getType(), dataToSend);
                stopAndWait.broadcastMessage(source, newMsg, mde, sequenceNum,  hopsToLive-1, 0);
                try {
                    sendingQueue.put(newMsg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        // Handle messages from the server / audio framework
        public void run(){
            while(true) {
                try{
                    Message m = receivedQueue.take();
                    if (m.getType() == MessageType.BUSY){
                        free = false;// The channel is busy (A node is sending within our detection range)
                    } else if (m.getType() == MessageType.FREE){
                        free = true;// The channel is no longer busy (no nodes are sending within our detection range)
                    } else if (m.getType() == MessageType.DATA){ // We received a data frame!
                        ack = stopAndWait.processPacket(m);   //process packet and change the data of the Message object
                        if(ack!= null) {
                            if(ack.getType() == MessageType.DATA){
                                sendingQueue.put(ack);
                            } else {
                                sendingQueue.put(ack);
                                forwardPacket(m);
                            }
                        }    //put the acknowledgement in the sending queue
                    } else if (m.getType() == MessageType.DATA_SHORT) { // We received a short data frame!

                        Message response = stopAndWait.processPacket(m);
                        if (response != null){
                            sendingQueue.put(response);
                        }
                    } else if (m.getType() == MessageType.DONE_SENDING){ // This node is done sending
                    } else if (m.getType() == MessageType.HELLO){ // Server / audio framework hello message. You don't have to handle this
                        System.out.println("\n\n ~~ Welcome ~~");
                        System.out.println(String.format("Hello User %s. Setting up the network... ", addressing.getCurrentNode()));
                        addressing();
                        System.out.println("Please wait while the network is being established... ");
                        initiateRouting();
                    } else if (m.getType() == MessageType.SENDING){ // This node is sending
                    } else if (m.getType() == MessageType.END){ // Server / audio framework disconnect message. You don't have to handle this
                        System.exit(0);
                    }
                } catch (InterruptedException e){
                    System.err.println("Failed to take from queue: "+e);
                }                
            }
        }
    }


}


