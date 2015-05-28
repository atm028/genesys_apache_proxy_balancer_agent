package GenesysApacheProxyBalancerAgent;

import com.genesyslab.platform.commons.collections.KeyValuePair;
import com.genesyslab.platform.commons.protocol.Endpoint;
import com.genesyslab.platform.commons.protocol.Message;
import com.genesyslab.platform.commons.protocol.MessageHandler;
import com.genesyslab.platform.commons.protocol.ProtocolException;
import com.genesyslab.platform.reporting.protocol.StatServerProtocol;
import com.genesyslab.platform.reporting.protocol.statserver.*;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestOpenStatistic;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestCloseStatistic;
import com.genesyslab.platform.reporting.protocol.statserver.events.EventInfo;
import com.genesyslab.platform.reporting.protocol.statserver.events.EventError;
import java.util.*;

class StatServerManager {
    ApacheConfReader acr;
    ConfigServerManager csm;
    boolean run;
    StatServerProtocol proto;
    String tenantName;
    String tenantPswd;

    private Map<Integer, SubscriptionInfo> subscriptions = new HashMap<>();
    private int subscription_id;

    MessageHandler statserverMessageHandler = new MessageHandler() {
        @Override
        public void onMessage(Message message) {
            System.out.println(message);
            if(message.messageId() == 1) {
                EventError event = (EventError)message;
                System.out.println(event);
                subscriptions.remove(event.getReferenceId());
            }
            if(message.messageId() == 2) {
                try {
                    EventInfo event = (EventInfo)message;
                    subscriptions.get(event.getReferenceId()).node = csm.getNodeNameBySubscriptionName(subscriptions.get(event.getReferenceId()).name);
                    KeyValuePair option = csm.getSubscriptionBySubscriptionName(subscriptions.get(event.getReferenceId()).name);
                    subscriptions.get(event.getReferenceId()).counter = option.getTKVValue().getPair("Counter").getStringValue();
                    subscriptions.get(event.getReferenceId()).threshold = option.getTKVValue().getPair("threshold").getStringValue();
                    subscriptions.get(event.getReferenceId()).value = event.getStringValue();
                } catch(IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public List<SubscriptionInfo> getSubscriptions() {
        List<SubscriptionInfo> ret = new ArrayList<>();
        for(Integer key : subscriptions.keySet()) {
            ret.add(subscriptions.get(key));
        }
        return ret;
    }

    public StatServerManager(
            ApacheConfReader acr, 
            ConfigServerManager csm, 
            String addr, 
            int port, 
            String name,
            String tenantName,
            String tenantPswd) {

        this.tenantName = tenantName;
        this.tenantPswd = tenantPswd;
        this.acr = acr;
        this.csm = csm;
        this.run = true;
        this.proto = new StatServerProtocol(new Endpoint(name, addr, port));
        this.proto.setClientName("GenesysApacheProxyBalancerAgentStatServer");
        this.proto.setMessageHandler(statserverMessageHandler);
    }

    public void close() {
        this.run = false;
    }

    private void initStatistics() {

    }

    public void restartSubscribe() {
        //iterate through nodes in config file and get application names from config cluster
        //using the received name create subscription and notification for each subscription
        for(Integer key : subscriptions.keySet()) {
            RequestCloseStatistic req = RequestCloseStatistic.create();
            req.setStatisticId(key.intValue());
            try {
                this.proto.send(req);
            } catch (ProtocolException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("Cannot create subscription");
            }
        }

        subscriptions = new HashMap<>();
        subscription_id = 0;
        for(KeyValuePair option : csm.ors_statistics.keySet()) {
            System.out.println(option.getStringKey());
            createSubscription(this.tenantName, this.tenantPswd, option.getStringKey());
        }
    }

    public void open() {
        try {
            this.proto.open();
            restartSubscribe();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
    }

    private void createSubscription(
            String tenantName,
            String tenantPswd,
            String statisticType) {

        StatisticObject object = StatisticObject.create();
        object.setObjectId(tenantName);
        object.setObjectType(StatisticObjectType.Tenant);
        object.setTenantName(tenantName);
        object.setTenantPassword(tenantPswd);

        StatisticMetric metric = StatisticMetric.create();
        metric.setStatisticType(statisticType);

        Notification notification = Notification.create();
        notification.setMode(NotificationMode.Immediate);
        notification.setFrequency(60);

        RequestOpenStatistic request = RequestOpenStatistic.create();
        request.setStatisticObject(object);
        request.setStatisticMetric(metric);
        request.setNotification(notification);
        request.setReferenceId(subscription_id);
        subscriptions.put(subscription_id, new SubscriptionInfo(statisticType));
        subscription_id++;
        try {
            this.proto.send(request);
        } catch (ProtocolException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Cannot create subscription");
        }
    }
}
