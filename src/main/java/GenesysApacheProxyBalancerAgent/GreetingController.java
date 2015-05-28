package GenesysApacheProxyBalancerAgent;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping("/")
    @ResponseBody
    public ArrayList<Greeting> greeting(@RequestParam(value="name", defaultValue="Mister") String name) {
        ArrayList<Greeting> res = new ArrayList<>();
        res.add(new Greeting(counter.incrementAndGet(), String.format(template, name)));
        res.add(new Greeting(counter.incrementAndGet(), String.format(template, name)));
        return res;
    }

    @RequestMapping("/subscribe/start")
    @ResponseBody String subscribe_restart_handler() {
        GenesysApacheProxyBalancerAgentApplication.stat_server.restartSubscribe();
        return "OK";
    }
}
