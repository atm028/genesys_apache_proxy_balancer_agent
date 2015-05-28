package GenesysApacheProxyBalancerAgent;

import com.genesyslab.platform.commons.collections.KeyValueCollection;
import com.genesyslab.platform.commons.protocol.*;
import com.genesyslab.platform.configuration.protocol.ConfServerProtocol;
import com.genesyslab.platform.configuration.protocol.confserver.events.EventObjectsRead;
import com.genesyslab.platform.configuration.protocol.confserver.requests.objects.RequestReadObjects;
import com.genesyslab.platform.configuration.protocol.obj.ConfObject;
import com.genesyslab.platform.configuration.protocol.types.CfgAppType;
import com.genesyslab.platform.configuration.protocol.types.CfgObjectType;
import com.genesyslab.platform.applicationblocks.com.objects.CfgPortInfo;
import com.genesyslab.platform.configuration.protocol.obj.ConfStructure;
import com.genesyslab.platform.configuration.protocol.obj.ConfStructureCollection;
import com.genesyslab.platform.commons.collections.KVList;
import com.genesyslab.platform.commons.collections.KeyValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.lang.IllegalArgumentException;
import org.json.simple.JSONObject;

public class ConfigServerManager {
    private static ConfServerProtocol proto;
    private static List<ConfObject> hosts = new ArrayList<>();
    private static Map<AppInfoPair, ConfObject> ors_hosts = new HashMap<>();
    public static Map<KeyValuePair, String> ors_statistics = new HashMap<>();
    private static boolean init_done = false;
    private static String stat_server;

    static MessageHandler handler = new MessageHandler() {
        @Override
        public void onMessage(Message message) {
            EventObjectsRead event = (EventObjectsRead) message;
            //handle ORS applications
            if(event.getObjectType() == CfgObjectType.CFGApplication.asInteger()) {
                List<ConfObject> objects = event.getObjects();
                for(int i = 0; i < objects.size(); i++) {
                    ConfObject obj = objects.get(i);
                    if(CfgAppType.CFGOrchestrationServer.asInteger() == (int)obj.getPropertyValue("type")) {
                        String hostDBID = obj.getPropertyValue("serverInfo").toString().split("\n\t")[1].split("=")[1];
                        hostDBID = hostDBID.replaceAll(" ", "");
                        Integer http_port = new Integer(0);
                        ConfStructureCollection coll = (ConfStructureCollection)obj.getPropertyValue("portInfos");
                        for(int p = 0; p < coll.size(); p++) {
                            ConfStructure rec = coll.get(p);
                            if(rec.getPropertyValue("id").toString().equalsIgnoreCase("http")) {
                                http_port = new Integer(rec.getPropertyValue("port").toString());
                                System.out.println("http_port: "+rec.getPropertyValue("port"));
                            }
                        }
                        int dbid = Integer.parseInt(hostDBID);
                        for(int j = 0; j < hosts.size(); j++) {
                            if(dbid == hosts.get(j).getObjectDbid()) {
                                System.out.println("handler: "+obj.getPropertyValue("name")+":"+http_port);
                                ors_hosts.put(
                                        new AppInfoPair(obj.getPropertyValue("name").toString(), http_port),
                                        hosts.get(j));
                            }
                        }
                    }
                    if(
                            CfgAppType.CFGStatServer.asInteger() == (int)obj.getPropertyValue("type") && 
                            stat_server.equalsIgnoreCase((String)obj.getPropertyValue("name"))) {
                        KVList options = (KVList)obj.getPropertyValue("options");
                        for(Iterator it = options.iterator(); it.hasNext();) {
                            KeyValuePair option = (KeyValuePair)it.next();
                            KeyValuePair sub_category = option.getTKVValue().getPair("JavaSubCategory");
                            if(sub_category != null && sub_category.getStringValue().equalsIgnoreCase("ORSStatExtension.jar:Calculator")) {
                                ors_statistics.put(option, option.getTKVValue().getPair("Nodes").getStringValue());
                            }
                        }
                    }
                }
                System.out.println("ConfigServerManager init done");
                init_done = true;
                for(KeyValuePair option : ors_statistics.keySet()) {
                    System.out.println("ConfigServerManager: "+option.getStringKey());
                }
            }
            //handle hosts
            else if(event.getObjectType() == CfgObjectType.CFGHost.asInteger()) {
                hosts.addAll(event.getObjects());
                KeyValueCollection filter = new KeyValueCollection();
                filter.addObject("type", CfgAppType.CFGOrchestrationServer.asInteger().toString());
                CfgObjectType objectType = CfgObjectType.CFGApplication;
                RequestReadObjects request = RequestReadObjects.create(objectType.asInteger(), filter);
                try {
                    proto.send(request);
                } catch (ProtocolException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    ConfigServerManager(String addr, int port, String name, String user, String passwd, String stat_server_name) {
        this.stat_server = stat_server_name;
        System.out.println("ConfigServerManager statServer "+stat_server_name);
        this.proto = new ConfServerProtocol(new Endpoint(addr, port));
        this.proto.setClientApplicationType(CfgAppType.CFGSCE.asInteger());
        this.proto.setClientName("default");
        this.proto.setUserName(user);
        this.proto.setUserPassword(passwd);
        this.proto.setMessageHandler(handler);
    }

    public void open() {
        try {
            proto.open();
            KeyValueCollection filter = new KeyValueCollection();
            CfgObjectType objectType = CfgObjectType.CFGHost;
            RequestReadObjects request = RequestReadObjects.create(objectType.asInteger(), filter);
            proto.send(request);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Cannot open connection");
        } catch (RegistrationException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Cannot open connection");
        } catch (ProtocolException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Cannot open connection");
        }
    }

    public void close() {
       try {
           proto.close();
       } catch (InterruptedException e) {
           e.printStackTrace();
       } catch (ProtocolException e) {
           e.printStackTrace();
       }
    }

    static boolean isInit() { return init_done; }

    public static ConfObject getOrsNodeByAppName(String app_name) {
        if(init_done == false) throw new IllegalArgumentException("init_done");
        ConfObject ret = null;
        for(Map.Entry<AppInfoPair, ConfObject> entry : ors_hosts.entrySet()) {
            if(entry.getKey().appName.equalsIgnoreCase(app_name)) {
                ret = entry.getValue();
            }
        }
        if(ret != null) return ret;
        throw new IllegalArgumentException(app_name + " not found");
    }

    public static String getAppNameByHostName(String host_name, Integer port) {
        System.out.println("getAppNameByHostName: "+host_name+":"+port.intValue());
        for(Map.Entry<AppInfoPair, ConfObject> entry : ors_hosts.entrySet()) {

            System.out.println(
                    entry.getValue().getPropertyValue("IPaddress").toString()+ ":"+entry.getKey().appHttpPort);
            
            if(
                    entry.getValue().getPropertyValue("IPaddress").toString().equalsIgnoreCase(host_name.split(":")[0]) &&
                    entry.getKey().appHttpPort.equals(port)
                ) {
                return entry.getKey().appName;
            } 
        }
        return "none";
    }

    public static JSONObject getHosts() {
        JSONObject ret = new JSONObject();
        for(Map.Entry<AppInfoPair, ConfObject> entry : ors_hosts.entrySet()) {
            ret.put(entry.getKey().appName, entry.getValue().getPropertyValue("IPaddress").toString());
        }
        return ret;
    }

    public static List<String> getHostsDBIds() {
        List<String> ret = new ArrayList<>();
        for(int j = 0; j < hosts.size(); j++) {
            ret.add(hosts.get(j).getObjectDbid().toString());
        }
        return ret;
    }

    KeyValuePair getSubscriptionBySubscriptionName(String name) {
        for(KeyValuePair option : ors_statistics.keySet()) {
            if(option.getStringKey().equalsIgnoreCase(name)) {
                return option;
            }
        }
        throw new IllegalArgumentException("There is no such name");
    }

    String getNodeNameBySubscriptionName(String name) {
        for(KeyValuePair option : ors_statistics.keySet()) {
            if(option.getStringKey().equalsIgnoreCase(name)) {
                return ors_statistics.get(option).toString();
            }
        }
        throw new IllegalArgumentException("There is no such name");
    }
}
