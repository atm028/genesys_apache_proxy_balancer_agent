package GenesysApacheProxyBalancerAgent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import java.lang.Thread;

//@SpringBootApplication
//@Configuration
@ComponentScan
@EnableAutoConfiguration

public class GenesysApacheProxyBalancerAgentApplication {
    public static ApacheConfReader cfg;
    public static ConfigServerManager csm;
    public static StatServerManager stat_server;

    private static String apache_file, sc_host, sc_user, sc_password;
    private static String st_host, st_name, st_tenant;
    private static int st_port;
    private static int port;

    public static void main(String[] args) {
        System.out.println(args);
        int found_args = 0;

        for(int i = 0; i < args.length; i++) {
            String[] arg = args[i].split("=");
            if(arg.length >= 2) {
                switch (arg[0]) {
                    case "apache_file":
                        apache_file = arg[1];
                        found_args++;
                        break;
                    case "sc_host":
                        sc_host = arg[1];
                        found_args++;
                        break;
                    case "sc_port":
                        port = Integer.parseInt(arg[1]);
                        found_args++;
                        break;
                    case "sc_user":
                        sc_user = arg[1];
                        found_args++;
                        break;
                    case "sc_password":
                        sc_password = arg[1];
                        found_args++;
                        break;
                    case "st_host":
                        st_host = arg[1];
                        found_args++;
                        break;
                    case "st_name":
                        st_name = arg[1];
                        found_args++;
                        break;
                    case "st_tenant":
                        st_tenant = arg[1];
                        found_args++;
                        break;
                    case "st_port":
                        st_port = Integer.parseInt(arg[1]);
                        found_args++;
                        break;
                }
            }
        }

        if(found_args < 8) {
            System.out.println("Not enough parameters");
            String help = "Usage:\n";
            help += "apache_file=path to apache config file\n";
            help += "sc_host=hostanem of ConfigServer\n";
            help += "sc_port=port of ConfigServer\n";
            help += "sc_user=ConfigServer user name\n";
            help += "sc_password= ConfigServer password\n";
            help += "st_host= StatServer host\n";
            help += "st_port= StatServer port\n";
            help += "st_name= StatServer app name\n";
            help += "st_tenant= TenantName for statistics\n";
            help += "st_tenant_password= StatServer password\n";
            System.out.println(help);
            System.exit(0);
        }
        System.out.println("Start ApacheConfReader");
        cfg = new ApacheConfReader(apache_file);
        System.out.println("Start ConfigServerManager");
        csm = new ConfigServerManager(sc_host, port, "default", sc_user, sc_password, st_name);
        System.out.println("ConfigServerManager opening");
        csm.open();
        System.out.println("Start StatServerManager");
        stat_server = new StatServerManager(cfg, csm, st_host, st_port, st_name, st_tenant, "");
        stat_server.open();
        System.out.println("Start application");
        SpringApplication.run(new Object[]{GenesysApacheProxyBalancerAgentApplication.class}, args);
    }
}
