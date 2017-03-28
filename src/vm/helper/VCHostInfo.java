package vm.helper;

import com.vmware.vim25.*;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/*
 * hostName 为主机的名字
 * getInfo方法 是该类中的主要方法
 */
public class VCHostInfo extends VCTaskBase {
	private static String hostName;
	private static ManagedObjectReference hostMoref;

	public static void setHostName(String name) {
		hostName = name;
	}


	private static void validateTheInput() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
		if (hostName == null) {
			throw new IllegalArgumentException("Must specify the host name");
		} else {
			ManagedObjectReference container = serviceContent.getRootFolder();
			hostMoref =
					VCHelper.inContainerByType(container, "HostSystem").get(hostName);
			if (hostMoref == null) {
				throw new IllegalArgumentException("There does not exists " + hostName + " such a host!!!");
			}
		}
	}

	//获取主机的cpu、mem的信息通过HostListSummary类
	private static String getInfo() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
		//cpu 单位为Mhz 可能需要更改为Ghz
		int cpuTotalSize;
		int cpuUsedSize;
		//double cpuUsedPercent;

		long memTotalSize; //单位为b
		int memUsedSize; //单位为Mb
		//double memUsedPercent;

		StringBuilder JsonHostInfo = new StringBuilder();
		JsonHostInfo.append("[");
		//String  info=null;
		String[] infoName = {"summary"};
		ManagedObjectReference container = serviceContent.getRootFolder();
		Map<ManagedObjectReference, Map<String, Object>> hostMap = VCHelper.inContainerByType(container, "HostSystem", infoName, new RetrieveOptions());
		Object[] hostSet = hostMap.keySet().toArray();
		for (Object m : hostSet) {
			if (((ManagedObjectReference) m).getValue().equalsIgnoreCase(hostMoref.getValue()) && ((ManagedObjectReference) m).getType().equals(hostMoref.getType())) {
				Map<String, Object> infoMap = hostMap.get(m);
				HostListSummary hostSummary = (HostListSummary) infoMap.get(infoName[0]);
				HostHardwareSummary hardwareSummary = hostSummary.getHardware();
				cpuTotalSize = hardwareSummary.getCpuMhz() * hardwareSummary.getNumCpuCores();
				memTotalSize = hardwareSummary.getMemorySize();
				HostListSummaryQuickStats quickStates = hostSummary.getQuickStats();
				cpuUsedSize = quickStates.getOverallCpuUsage();
				//cpuUsedPercent = 100.0*cpuUsedSize/cpuTotalSize ; 
				memUsedSize = quickStates.getOverallMemoryUsage();
				JsonHostInfo.append(String.format("{ \"CPU Total By Mhz\":%s, \"CPU Used By Mhz\":%s, \"Memory Size By B\":%s, \"Memory Used By Mb\":%s }",
						cpuTotalSize, cpuUsedSize, memTotalSize, memUsedSize));
				//System.out.println("CPU total:"+cpuTotalSize+"CPU ued "+quickStates.getOverallCpuUsage());
				//System.out.println("cpu used percent "+cpuUsedPercent);
				//System.out.println("mem size :"+memTotalSize+" used :"+memUsedSize);
			}
			//info = quickStates.getOverallMemoryUsage().toString();
		}
		JsonHostInfo.deleteCharAt(JsonHostInfo.length() - 2);
		JsonHostInfo.append("]");
		return JsonHostInfo.toString();
	}

	public static String run() throws KeyManagementException, NoSuchAlgorithmException, RuntimeFaultFaultMsg, InvalidLoginFaultMsg, InvalidLocaleFaultMsg, InvalidPropertyFaultMsg {
		if (!VCClientSession.IsConnected()) {
			VCClientSession.Connect();
		}
		init();
		validateTheInput();
		return getInfo();
	}
}
