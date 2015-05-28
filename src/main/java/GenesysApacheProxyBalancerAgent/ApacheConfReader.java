package GenesysApacheProxyBalancerAgent;

import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.lang.Runtime;

public class ApacheConfReader {
    private List<Integer> nodesIndexArray;
    private List<Integer> proxyOptionsArray;
    private List<String> cfgFileLines;
    private String fileName;

    public ApacheConfReader(String file_name) {
        fileName = file_name;
        parse();
    }

    public List<BalancerMember> getClusterNodes() {
        List<BalancerMember> ret = new ArrayList<>();
        for(Integer key : nodesIndexArray) {
            try {
                BalancerMember node = new BalancerMember(cfgFileLines.get(key));
                System.out.println("getClusterNodes: "+node.addr+":"+node.port);
                ret.add(node);
            } catch(IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public List<String> getClusterConfig() {
        List<String> ret = new ArrayList<>();
        for(Integer key : proxyOptionsArray)
            ret.add(cfgFileLines.get(key));
        return ret;
    }

    public void parse() {
       nodesIndexArray = new ArrayList<>();
       proxyOptionsArray = new ArrayList<>();
       cfgFileLines = new ArrayList<String>();
       try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferredReader = new BufferedReader(fileReader);
            String line = null;
            while((line = bufferredReader.readLine()) != null) {
                cfgFileLines.add(line);
            }

            boolean found = false;
            for(int i = 0; i < cfgFileLines.size(); i++) {
                if(cfgFileLines.get(i).contains("</Proxy")) found = false;
                if(found) {
                    String[] line_options = cfgFileLines.get(i).split(" ");
                    if(line_options[0].equalsIgnoreCase("BalancerMember")) {
                        nodesIndexArray.add(new Integer(i));
                    }
                    if(line_options[0].equalsIgnoreCase("ProxySet")) {
                        proxyOptionsArray.add(new Integer(i));
                    }
                }
                if(cfgFileLines.get(i).contains("Proxy balancer")) found = true;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> readFile(String file_name) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file_name));
        List<String> lines = new ArrayList<String>();
        String line = reader.readLine();
        while(line != null) {
            lines.add(line);
            line = reader.readLine();
        }
        return lines;
    }

    public void updateNode(JSONObject json) {
        boolean found = false;
        for(Integer key : nodesIndexArray) {
            BalancerMember node = new BalancerMember(cfgFileLines.get(key));
            if(
                    json.get("name").toString().compareToIgnoreCase(node.name) == 0 &&
                    json.get("addr").toString().compareToIgnoreCase(node.addr) == 0) {
                node.options = json.get("options").toString();
                cfgFileLines.remove(key);
                cfgFileLines.add(key, node.toString());
                found = true;
                break;
            }
        }

        if(!found) {
            BalancerMember node = new BalancerMember();
            node.addr = json.get("addr").toString();
            node.name = json.get("name").toString();
            node.port = new Integer(json.get("port").toString()).intValue();
            node.options = json.get("options").toString();
            Integer new_key = nodesIndexArray.get(nodesIndexArray.size() - 1) + 1;
            cfgFileLines.add(new_key, node.toString());
        }

        write();
    }

    public void updateBalancerOptions(JSONObject json) {
        for(Integer key : proxyOptionsArray) {
            boolean changed = false;
            String[] options = cfgFileLines.get(key).split(" ");
            for(int i = 0; i < options.length; i++) {
                if(json.get("name").toString().compareToIgnoreCase(options[i].split("=")[0]) == 0) {
                    options[i] = json.get("name") + "=" + json.get("value");
                    changed = true;
                    break;
                }
            }
            if(changed) {
                String updt_option = "";
                for(String option : options) 
                    updt_option += option+" ";
                cfgFileLines.remove(key);
                cfgFileLines.add(key, updt_option);
                break;
            }
        }
        write();
    }

    public void write() {
        try {
            FileOutputStream out = new FileOutputStream(fileName);
            for(int i = 0; i < cfgFileLines.size(); i++) {
                String ln = cfgFileLines.get(i) + "\n";
                out.write(ln.getBytes());
            }
            out.close();
        } catch(FileNotFoundException e) {
            System.out.println("File not found");
        } catch(SecurityException e) {
            System.out.println("Security exception");
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void restart() {
        try {
            Runtime.getRuntime().exec("apache2ctl -k graceful");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
