package Implementation;

import client.Message;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class MAC {
    public int sendBoundary = 37;
    public int resendCount = 0;
    public int powerNum = 400;
    public BlockingQueue<Message> waitingSendQueue = new LinkedBlockingQueue<>(); // waiting queue
    public BlockingQueue<Message> readyToSendQueue = new LinkedBlockingQueue<>();
    boolean iterator = true;

    /**
     * Constructor responsible for adding a received message to waiting queue
     * and starting the thread of class MAC
     */
    public MAC(){

    }

    /**
     * Method for transmitting the packets in the medium
     * //@param m message received from the received buffer in MyProtocol class
     * implements exponential backoff. random number n is selected from between 0 and randomlimit
     * random limit is decided by powerNum^resendCount+1
     * if n ==1 then it returns true so it can be send
     */
    public boolean exponentialBackoff(){
        int randomLimit = (int) (Math.pow(powerNum, resendCount + 1) - 1);
        int n = (int) (Math.random() * randomLimit + 1);
        if (n == 1){
            return true;
        }else{
            return false;
        }
    }

    /** Method for transmitting Acknowledgment packets
     * @return true when the random number == 1
     */
    public boolean exponentialACK() {
        int randomLimit = (int) (Math.pow(200, 3) - 1);
        int n = (int) (Math.random() * randomLimit + 1);
        if (n == 1){
            return true;
        }else{
            return false;
        }
    }
/**
 * increments the number of resends in the class
 */
    public void incrsResendCount(){
        this.resendCount++;
    }
/**
 * resets the resend counter to 0
 */
    public void resetResendCount(){
        this.resendCount = 1;
    }

    public void decrsResendCount() {
        this.resendCount--;
    }

    public void setResendCount(int val) {
        this.resendCount = val;
    }

    public void setPowerNum(int powerNum) {
        this.powerNum = powerNum;
    }

    public int getPowerNum() {
        return this.powerNum;
    }
}
