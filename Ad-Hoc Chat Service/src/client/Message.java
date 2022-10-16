package client;

import java.nio.ByteBuffer;

public class Message implements Comparable<Message>{
    private MessageType type;
    private ByteBuffer data;

    public Message(MessageType type){
        this.type = type;
    }

    public Message(MessageType type, ByteBuffer data){
        this.type = type;
        this.data = data;
    }

    public MessageType getType(){
        return type;
    }

    public void setData(ByteBuffer data){
        this.data = data;
    }
    public ByteBuffer getData(){
        return data;
    }

    @Override
    public int compareTo(Message o) {
//        int[] message1 = new int[data.array().length];
//        int[] message2 = new int[o.getData().array().length];
//        for (int i = 0; i<data.array().length;i++){
//            message1[i]  =  data.array()[i];
//        } for (int i = 0; i<o.getData().array().length;i++){
//            message1[i]  =  o.getData().array()[i];
//        }
//        int message1Sum = 0;
//        for (int i = 0; i<message1.length;i++){
//            message1Sum =  message1Sum + message1[i];
//        }  int message2Sum = 0;
//        for (int i = 0; i<message2.length;i++){
//            message2Sum =  message2Sum + message2[i];
//        }
        int seqNum = Byte.toUnsignedInt(data.get(2));
        int src = bitExtracted(Byte.toUnsignedInt(data.get(0)),4,5);
        int sndSeqNum = Byte.toUnsignedInt(o.getData().get(2));
        int sndSrc = bitExtracted(Byte.toUnsignedInt(o.getData().get(0)),4,5);
        return (seqNum+src) - (sndSeqNum+sndSrc);
    }

    public int bitExtracted(int number, int k, int p)
    {
        return (((1 << k) - 1) & (number >> (p - 1)));
    }
}