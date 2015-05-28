package GenesysApacheProxyBalancerAgent;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.lang.IllegalArgumentException;

/**
 * Created by tmorozov on 5/8/2015.
 */
public class BalancerMember {
        public int id;
        public String addr;
        public String name;
        int port;
        String options;

        public BalancerMember() {
            addr = null;
            name = null;
            port = 0;
            options = null;
        }
        public BalancerMember(String str) {
            String[] lines = str.split(" ");
            if(lines.length < 2)
                throw new IllegalArgumentException("Incorrect BalancerMember string");
            if(!lines[0].equalsIgnoreCase("BalancerMember"))
                throw new IllegalArgumentException("Not a BalancerMember string");
            String[] addr_parts = lines[1].replaceAll("http://", "").split(":");
            if(addr_parts.length < 2)
                throw new IllegalArgumentException("Incorrect BalancerMember address");
            addr = addr_parts[0];
            port = Integer.parseInt(addr_parts[1]);
            if(lines.length > 2) {
                for(int i = 2; i < lines.length; i++) {
                    options += lines[i]+" ";
                }
            }
        }

        public String toString() {
            String ret = "BalancerMember http://"+addr+":"+ port + " " + options;
            return ret;
        }

        public JSONObject getJSON() {
                JSONObject json = new JSONObject();
                json.put("id", id);
                json.put("addr", addr);
                json.put("port", port);
                json.put("name", name);
                json.put("options", options);
                return json;
        }
}
