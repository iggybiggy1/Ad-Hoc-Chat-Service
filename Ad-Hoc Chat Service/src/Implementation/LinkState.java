package Implementation;

import java.nio.ByteBuffer;

public class LinkState {
   private int cost;
   private int destination;
   private int nextHop;

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setDestination(int neighbour){
        this.destination = neighbour;
    }

    public void setNextHop(int nextHop) {
        this.nextHop = nextHop;
    }

    public int getNextHop(){
        return this.nextHop;
    }

    public int getCost(){
        return this.cost;
    }

    public int getDestination(){
        return this.destination;

    }}
