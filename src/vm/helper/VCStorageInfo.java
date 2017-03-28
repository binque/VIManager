package vm.helper;

import com.vmware.vim25.*;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class VCStorageInfo extends VCTaskBase {
	private static String getInfo() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
		long capability;//单位是b
		long freeSpace;//单位是b
		StringBuilder JsonStorage = new StringBuilder();
		JsonStorage.append('[');
		String[] infoName = {"summary"};
		ManagedObjectReference container = serviceContent.getRootFolder();
		Map<ManagedObjectReference, Map<String, Object>> datastoreMap = VCHelper.inContainerByType(container, "Datastore", infoName, new RetrieveOptions());
		for (Map<String, Object> value : datastoreMap.values()) {
			DatastoreSummary datastoreSummary = (DatastoreSummary) value.get(infoName[0]);

			capability = datastoreSummary.getCapacity();
			freeSpace = datastoreSummary.getFreeSpace();
			JsonStorage.append(String.format("{ \"Name of the datacenter\":%s, \"Total capability By B\":%s, \"Free space By B\":%s}", datastoreSummary.getName(), capability, freeSpace));
			//System.out.println(" Name "+datastoreSummary.getName());
			//System.out.println(datastoreSummary.getDatastore().getType()+" "+datastoreSummary.getDatastore().getValue()+" "+datastoreSummary.getUncommitted()+" "+datastoreSummary.getUrl());

		}
		JsonStorage.deleteCharAt(JsonStorage.length() - 2);
		JsonStorage.append("]");
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
