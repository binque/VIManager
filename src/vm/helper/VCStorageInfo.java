package vm.helper;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.HostConfigSummary;
import com.vmware.vim25.InvalidLocaleFaultMsg;
import com.vmware.vim25.InvalidLoginFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RuntimeFaultFaultMsg;

public class VCStorageInfo extends VCTaskBase{
	public String getInfo() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg{
		long capability=0;//单位是b
		long freeSpace=0;//单位是b
		StringBuilder JsonStorage = new StringBuilder();
	    JsonStorage.append('[');
		String [] infoName={"summary"};
		ManagedObjectReference container = VCClientSession.getServiceContent().getRootFolder();
		Map<ManagedObjectReference, Map<String, Object>> datastoreMap = VCHelper.inContainerByType(container, "Datastore", infoName, new RetrieveOptions());
		Object[] dataSet = datastoreMap.keySet().toArray();
		for(Object key:dataSet){
			DatastoreSummary datastoreSummary = (DatastoreSummary) datastoreMap.get(key).get(infoName[0]);
			
				capability = datastoreSummary.getCapacity();
				freeSpace = datastoreSummary.getFreeSpace();
				JsonStorage.append(String.format("{ \"Name of the datacenter\":%s, \"Total capability By B\":%s, \"Free space By B\":%s}", datastoreSummary.getName(),capability,freeSpace));
				//System.out.println(" Name "+datastoreSummary.getName());
				//System.out.println(datastoreSummary.getDatastore().getType()+" "+datastoreSummary.getDatastore().getValue()+" "+datastoreSummary.getUncommitted()+" "+datastoreSummary.getUrl());

		}
		JsonStorage.deleteCharAt(JsonStorage.length() - 2);
		JsonStorage.append("]");
		return JsonStorage.toString();
	}
	
	public String run() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, KeyManagementException, NoSuchAlgorithmException, InvalidLoginFaultMsg, InvalidLocaleFaultMsg{
		 if (!VCClientSession.IsConnected()) {
	            VCClientSession.Connect();
	        }
		 return getInfo();
	}
}
