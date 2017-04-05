package vm.helper;

import com.vmware.vim25.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.Map;

public class VCStorageInfo extends VCTaskBase {
    private String getStorageInfo() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        long capability;//单位是b
        long freeSpace;//单位是b
        JSONArray JsonStorage = new JSONArray();
        String[] infoName = {"summary"};
        ManagedObjectReference container = serviceContent.getRootFolder();
        Map<ManagedObjectReference, Map<String, Object>> datastoreMap = VCHelper.inContainerByType(serviceContent, vimPort, container, "Datastore", infoName, new RetrieveOptions());
        for (Map<String, Object> value : datastoreMap.values()) {
            DatastoreSummary datastoreSummary = (DatastoreSummary) value.get(infoName[0]);

            capability = datastoreSummary.getCapacity();
            freeSpace = datastoreSummary.getFreeSpace();
            JSONObject jo = new JSONObject();
            jo.put("Name of the datacenter", datastoreSummary.getName());
            jo.put("Total capability By B", capability);
            jo.put("Free space By B", freeSpace);
            JsonStorage.add(jo);
            //logger.info(" Name "+datastoreSummary.getName());
            //logger.info(datastoreSummary.getDatastore().getType()+" "+datastoreSummary.getDatastore().getValue()+" "+datastoreSummary.getUncommitted()+" "+datastoreSummary.getUrl());

        }
        return JsonStorage.toString();
    }

    public String run() throws Exception {
        try {
            init();
            return getStorageInfo();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            vcClientSession.Disconnect();
        }
    }
}
