package GenesysApacheProxyBalancerAgent;

public class AppInfoPair {
        public String appName;
        public Integer appHttpPort;

        public AppInfoPair(String appName, Integer port) {
            this.appName = appName;
            this.appHttpPort = port;
        }
};
