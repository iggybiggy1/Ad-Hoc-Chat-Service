package Implementation;

public class MyRoute {
    private int nextHop;
    private int cost;


    public MyRoute(){
        cost = 0;
    }

    /**Sets up the nextHop to the neighbour
     * @param nextHop - node between the route */
    public void setNextHop(int nextHop) {
        this.nextHop = nextHop;
    }

    /**Assigns the cost field wiht the given value
     * @param cost - cost of the link to the neighbour
     * */
    public void setCost(int cost){
        this.cost = cost;
    }

    /**
     * @return cost - the cost of the link of this route*/
    public int getCost(){
        return cost;
    }

    /**
     * @return nextHop - the nextHop for this route
     * */
    public int getNextHop(){
        return nextHop;
    }
}
