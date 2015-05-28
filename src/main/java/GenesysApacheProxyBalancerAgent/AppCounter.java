package GenesysApacheProxyBalancerAgent;


/**
 * Created by tmorozov on 5/4/2015.
 */
public class AppCounter {
    private int counterID;

    public void setCounterThreshhold(int counterThreshhold) {
        this.counterThreshhold = counterThreshhold;
    }

    private int counterThreshhold;


    public int getCounterID() {
        return counterID;
    }

    public int getCounterThreshhold() {
        return counterThreshhold;
    }

    public AppCounter(int counterID, int counterThreshhold) {
        this.counterID = counterID;
        this.counterThreshhold = counterThreshhold;
    }

}
