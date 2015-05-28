package GenesysApacheProxyBalancerAgent;

class SubscriptionInfo {
    public String name;
    public String node;
    public String value;
    public String threshold;
    public String counter;

    public SubscriptionInfo(String name, String node, String value, String threshold) {
        this.name = name;
        this.node = node;
        this.value = value;
        this.threshold = threshold;
        this.counter = counter;
    }

    public SubscriptionInfo(String name) {
        this.name = name;
    }

}
