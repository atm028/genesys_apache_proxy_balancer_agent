package GenesysApacheProxyBalancerAgent;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by tmorozov on 5/4/2015.
 */

@RestController
public class AppCounterController {
    ArrayList<AppCounter> counterList = new ArrayList<>();
    HashMap<String, Integer> countersID = new HashMap<>();

    public AppCounterController() {
        countersID.put("active-sessions", 0);
        countersID.put("pending-sessions", 1);
        countersID.put("create-session-time", 2);
        countersID.put("document-processing-time", 3);
        countersID.put("scxml-event-queuing-time", 4);
        countersID.put("http-fetch-time", 5);
        countersID.put("esp-fetch-time", 6);
        countersID.put("urs-fetch-time", 7);
        countersID.put("openstat-time", 8);
        countersID.put("udata-update-time", 9);
        countersID.put("cassandra-latency", 10);
        countersID.put("js-execution-time", 11);
        countersID.put("func-execution-time", 12);
        countersID.put("state-time", 13);
        countersID.put("creation-session-rate", 14);
    }

    private int getCounterID(String name) {
        Integer ret = countersID.get(name);
        if(ret != null) return ret.intValue();
        throw new IllegalArgumentException("Wrong counter name");
    }

    @RequestMapping(value = "/counter/add", method = RequestMethod.POST)
    public @ResponseBody String add(
            @RequestParam(value = "name", defaultValue = "none") String name,
            @RequestParam(value = "th", defaultValue = "0") int threshhold) {
        try {
            int counter_id = getCounterID(name);
            AppCounter ac = null;
            for(int i = 0; i < counterList.size(); i++) {
                if(counter_id == counterList.get(i).getCounterID()) {
                    ac = counterList.get(i);
                    ac.setCounterThreshhold(threshhold);
                }
            }
            if(ac == null) counterList.add(new AppCounter(counter_id, threshhold));
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        return "OK";
    }

    @RequestMapping(value = "/counter/config", method = RequestMethod.GET)
    public @ResponseBody ArrayList<String> config() {
        ArrayList<String> ret = new ArrayList<>();
        ret.addAll(countersID.keySet());
        return ret;
    }

    @RequestMapping(value = "counter/list", method = RequestMethod.GET)
    public @ResponseBody List<SubscriptionInfo> list() {
        return  GenesysApacheProxyBalancerAgentApplication.stat_server.getSubscriptions();
    }

    @RequestMapping(value = "/counter/get", method = RequestMethod.GET)
    public @ResponseBody String get(@RequestParam(value = "name", defaultValue = "none") String name) {
        try {
            int counter_id = getCounterID(name);
            for(int i = 0; i < counterList.size(); i++) {
                AppCounter ac = counterList.get(i);
                if(ac.getCounterID() == counter_id) return new Integer(ac.getCounterThreshhold()).toString();
            }
        } catch(IllegalArgumentException e) {
            return e.getMessage();
        }
        return "OK";
    }

    @RequestMapping(value = "/counter/delete", method = RequestMethod.PUT)
    public @ResponseBody String delete(@RequestParam(value = "name", defaultValue = "none") String name) {
        try {
            int counter_id = getCounterID(name);
            for (int i = 0; i < counterList.size(); i++) {
                AppCounter ac = counterList.get(i);
                if(ac.getCounterID() == counter_id) counterList.remove(i);
            }
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        return "OK";
    }
}
