package client;

import Implementation.*;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.BlockingQueue;


public class Client {
    
    private SocketChannel sock;
    private ReliableTransfer stopAndWait;

    public void printByteBuffer(ByteBuffer bytes, int bytesLength){
        System.out.print("DATA: ");
        for(int i=0; i<bytesLength; i++){
            System.out.print( Byte.toString( bytes.get(i) )+" " );
        }
        System.out.println();
    }

    public Client(String server_ip, int server_port, int frequency, BlockingQueue<Message> receivedQueue, BlockingQueue<Message> sendingQueue, ReliableTransfer stopAndWait, Addressing addressing, Reachability reachable){
        this.stopAndWait = stopAndWait;
        SocketChannel sock;
        Sender sender;
        Listener listener;
        try{
            sock = SocketChannel.open();
            sock.connect(new InetSocketAddress(server_ip, server_port));
            listener = new Listener( sock, receivedQueue );
            sender = new Sender( sock, sendingQueue);
            sender.sendConnect(frequency);
            listener.start();
            sender.start();
        } catch (IOException e){
            System.err.println("Failed to connect: "+e);
            System.exit(1);
        }     
    }

    private class Sender extends Thread {
        private BlockingQueue<Message> sendingQueue;
        private SocketChannel sock;
        private MAC mac;
        private boolean iterator = true;
        
        public Sender(SocketChannel sock, BlockingQueue<Message> sendingQueue){
            super();
            this.sendingQueue = sendingQueue;
            this.sock = sock;
        }

        private void sendData(ByteBuffer toSend, Message msg){
            boolean timedOut = true;
            mac = new MAC();
            mac.resetResendCount();
            int finalCounter = 0;
            if (msg.getType() == MessageType.DATA_SHORT){
                 while(timedOut) {
                        if (mac.exponentialACK()) {
                            try {
                                sock.write(toSend);
                                mac.resetResendCount();
                                break;
                            } catch (IOException e) {
                            }
                        } else {
                            mac.incrsResendCount();
                        }

                }
            } else {
                while (timedOut) {
                    if (mac.exponentialBackoff()) {
                        try {
                            int counter = 0;
                            //           System.out.println("MyProtocol, sendData - sending right now");
                            //           System.out.println("MyProtocol, sendData - iterations: "+ i);
                            sock.write(toSend);
                            mac.incrsResendCount();
                            if (!stopAndWait.enterTimeout(msg)) {
                                    finalCounter++;
                                if(finalCounter == 2) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        public int bitExtracted(int number, int k, int p)
        {
            return (((1 << k) - 1) & (number >> (p - 1)));
        }

        private void senderLoop(){
            while(sock.isConnected()){
                try{
                    Message msg = sendingQueue.take();
                    if( msg.getType() == MessageType.DATA || msg.getType() == MessageType.DATA_SHORT ){
                        ByteBuffer data = msg.getData();
                        data.position(0); //reset position just to be sure
                        int length = data.capacity(); //assume capacity is also what we want to send here!
                        ByteBuffer toSend = ByteBuffer.allocate(length+2);
                        if( msg.getType() == MessageType.DATA ){
                            toSend.put((byte) 3);
                        } else { // must be DATA_SHORT due to check above
                            toSend.put((byte) 6);
                        }
                        toSend.put((byte) length);
                        toSend.put(data);
                        toSend.position(0);
                        // System.out.println("Sending "+Integer.toString(length)+" bytes!");
                        sendData(toSend, msg);
                    }
                } catch(InterruptedException e){
                    System.err.println("Failed to take from sendingQueue: "+e);
                }
            }
        }

        public void sendConnect(int frequency){
            ByteBuffer buff = ByteBuffer.allocate(4);
            buff.put((byte) 9);
            buff.put((byte) ((frequency >> 16)&0xff));
            buff.put((byte) ((frequency >> 8)&0xff));
            buff.put((byte) (frequency&0xff));
            buff.position(0);
            try{
                sock.write(buff);
            } catch(IOException e) {
                System.err.println("Failed to send HELLO" );
            }            
        }

        public void run(){
            senderLoop();
        }

    }


    private class Listener extends Thread {
        private BlockingQueue<Message> receivedQueue;
        private SocketChannel sock;

        public Listener(SocketChannel sock, BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
            this.sock = sock;      
        }

        private ByteBuffer messageBuffer = ByteBuffer.allocate(1024);
        private int messageLength = -1;
        private boolean messageReceiving = false;
        private boolean shortData = false;

        private void parseMessage( ByteBuffer received, int bytesReceived ){
            // printByteBuffer(received, bytesReceived);

            try {
                for( int offset=0; offset < bytesReceived; offset++ ){
                    byte d = received.get(offset);
        
                    if( messageReceiving ){
                        if (messageLength == -1) {
                            messageLength = (int) d;
                            messageBuffer = ByteBuffer.allocate(messageLength);
                        } else {
                            messageBuffer.put( d );
                        }
                        if( messageBuffer.position() == messageLength ){
                            messageBuffer.position(0);
                            ByteBuffer temp = ByteBuffer.allocate(messageLength);
                            temp.put(messageBuffer);
                            temp.rewind();
                            if( shortData ){
                                receivedQueue.put( new Message(MessageType.DATA_SHORT, temp) );
                            } else {
                                receivedQueue.put( new Message(MessageType.DATA, temp) );
                            }                            
                            messageReceiving = false;
                        }
                    } else {
                        if ( d == 0x09 ){ // Connection successfull!
                            receivedQueue.put(new Message(MessageType.HELLO));
                        } else if ( d == 0x01 ){ // FREE
                            receivedQueue.put( new Message(MessageType.FREE) );
                        } else if ( d == 0x02 ){ // BUSY
                            receivedQueue.put( new Message(MessageType.BUSY) );
                        } else if ( d == 0x03 ){ // DATA!
                            messageLength = -1;
                            messageReceiving = true;
                            shortData = false;
                        } else if ( d == 0x04 ){ // SENDING
                            receivedQueue.put( new Message(MessageType.SENDING) );
                        } else if ( d == 0x05 ){ // DONE_SENDING
                            receivedQueue.put( new Message(MessageType.DONE_SENDING) );
                        }else if ( d == 0x06 ){ // DATA_SHORT
                            messageLength = -1;
                            messageReceiving = true;
                            shortData = true;
                        }else if ( d == 0x08 ){ // END, connection closing
                            receivedQueue.put( new Message(MessageType.END) );
                        }
                    } 
                }
                    
            } catch (InterruptedException e){
                System.err.println("Failed to put data in receivedQueue: "+e.toString());
            }
        }

        public void printByteBuffer(ByteBuffer bytes, int bytesLength){
            System.out.print("DATA: ");
            for(int i=0; i<bytesLength; i++){
                System.out.print( Byte.toString( bytes.get(i) )+" " );
            }
            System.out.println();
        }

        public void receivingLoop(){
            int bytesRead = 0;
            ByteBuffer recv = ByteBuffer.allocate(1024);
            try{
                while( sock.isConnected() ){
                    bytesRead = sock.read(recv);
                    if ( bytesRead > 0 ){
                        parseMessage( recv, bytesRead );
                    } else {
                        break;
                    }
                    recv.clear();              
                }
            } catch(IOException e){
                System.err.println("Error on socket: "+e );
            }       
            
        }

        public void run(){
            receivingLoop();
        }

    }
}