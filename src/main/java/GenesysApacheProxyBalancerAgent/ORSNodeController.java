package GenesysApacheProxyBalancerAgent;

import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Created by tmorozov on 5/6/2015.
 */
@RestController
public class ORSNodeController {
    @RequestMapping(value = "/node/list", method = RequestMethod.GET)
    public @ResponseBody List<JSONObject> nodelist() {
        List<JSONObject> ret = new ArrayList<>();
        List<BalancerMember> nodes = GenesysApacheProxyBalancerAgentApplication.cfg.getClusterNodes();
        for(BalancerMember node : nodes) {
            System.out.println("nodelist: "+node.addr+":"+node.port);
            node.name = GenesysApacheProxyBalancerAgentApplication.csm.getAppNameByHostName(node.addr, node.port);
            ret.add(node.getJSON());
        }
        return ret;
    }

    @RequestMapping(value = "/node/config", method = RequestMethod.GET)
    public @ResponseBody JSONObject cluster_config() {
        JSONObject ret = new JSONObject();
        List<String> options = GenesysApacheProxyBalancerAgentApplication.cfg.getClusterConfig();
        for(String option_line : options) {
            if(!option_line.equalsIgnoreCase("ProxySet")) {
                String[] option_set = option_line.split(" ");
                for(String option : option_set) {
                    String[] option_pair = option.split("=");
                    if(option_pair.length == 2) {
                        ret.put(option_pair[0], option_pair[1]);
                    }
                }
            }
        }
        return ret;
    }

    @RequestMapping(value = "/node/nupdate", method = RequestMethod.POST, headers = {"Content-type=application/json"})
    public @ResponseBody String node_update(@RequestBody final JSONObject json) {
        GenesysApacheProxyBalancerAgentApplication.cfg.updateNode(json);
        return "OK";
    }

    @RequestMapping(value = "/node/bupdate", method = RequestMethod.POST, headers = {"Content-type=application/json"})
    public @ResponseBody String balancer_update(@RequestBody final JSONObject json) {
        GenesysApacheProxyBalancerAgentApplication.cfg.updateBalancerOptions(json);
        return "OK";
    }

    @RequestMapping(value = "/node/names", method = RequestMethod.GET)
    public @ResponseBody JSONObject get_names_handler() {
        JSONObject ret = GenesysApacheProxyBalancerAgentApplication.csm.getHosts();
        return ret;
    }

    @RequestMapping(value = "/node/hosts", method = RequestMethod.GET)
    public @ResponseBody List<String> get_hosts_handler() {
        List<String> ret = GenesysApacheProxyBalancerAgentApplication.csm.getHostsDBIds();
        return ret;
    }
}
