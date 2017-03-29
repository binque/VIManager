package vm.helper;

import com.vmware.vim25.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class VCStorageInfo extends VCTaskBase {
	private static String getInfo() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
		long capability;//单位是b
		long freeSpace;//单位是b
		JSONArray JsonStorage = new JSONArray();
		String[] infoName = {"summary"};
		ManagedObjectReference container = serviceContent.getRootFolder();
		Map<ManagedObjectReference, Map<String, Object>> datastoreMap = VCHelper.inContainerByType(container, "Datastore", infoName, new RetrieveOptions());
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

	public static String run() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, KeyManagementException, NoSuchAlgorithmException, InvalidLoginFaultMsg, InvalidLocaleFaultMsg {
		if (!VCClientSession.IsConnected()) {
			VCClientSession.Connect();
		}
		init();
		return getInfo();
	}
}
