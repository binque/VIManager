package vm.helper;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import com.vmware.vim25.*;

/*
 * hostName 为主机的名字
 * getInfo方法 是该类中的主要方法
 */
public class VCHostInfo {
	private String hostName;
	private ManagedObjectReference hostMoref;
	
	public void setHostName(String name){
		this.hostName = name;
	}
	
	
	void validateTheInput() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg{
		if(hostName == null){
			throw new IllegalArgumentException("Must specify the host name"); 
		}
		else{
			ManagedObjectReference container = VCClientSession.getServiceContent().getRootFolder();
			hostMoref =
	                VCHelper.inContainerByType(container, "HostSystem").get(hostName);
			if(hostMoref==null){
				throw new IllegalArgumentException("There does not exists "+hostName+" such a host!!!");
			}
		}
	}
	
	boolean getTaskResultAfterDone(ManagedObjectReference task)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg,
            InvalidCollectorVersionFaultMsg {

        boolean retVal = false;

        // info has a property - state for state of the task
        Object[] result =
                VCClientSession.WaitForValues(task, new String[]{"info.state", "info.error"},
                        new String[]{"state"}, new Object[][]{new Object[]{
                        TaskInfoState.SUCCESS, TaskInfoState.ERROR}});

        if (result[0].equals(TaskInfoState.SUCCESS)) {
            retVal = true;
        }
        if (result[1] instanceof LocalizedMethodFault) {
            throw new RuntimeException(
                    ((LocalizedMethodFault) result[1]).getLocalizedMessage());
        }
        return retVal;
	}
	
	
	//获取主机的cpu、mem的信息通过HostListSummary类
	public String  getInfo() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg{
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
		String [] infoName={"summary"};
		ManagedObjectReference container = VCClientSession.getServiceContent().getRootFolder();
		Map<ManagedObjectReference, Map<String, Object>> hostMap = VCHelper.inContainerByType(container, "HostSystem", infoName, new RetrieveOptions());
		Object [] hostSet =  hostMap.keySet().toArray();
		for(Object m:hostSet){
			if(((ManagedObjectReference) m).getValue().equalsIgnoreCase(hostMoref.getValue())&&((ManagedObjectReference) m).getType().equals(hostMoref.getType())){
				Map<String, Object> infoMap = hostMap.get(m);
				HostListSummary hostSummary = (HostListSummary) infoMap.get(infoName[0]);
				HostHardwareSummary hardwareSummary = hostSummary.getHardware();
				cpuTotalSize = hardwareSummary.getCpuMhz()*hardwareSummary.getNumCpuCores();
				memTotalSize = hardwareSummary.getMemorySize();
				HostListSummaryQuickStats quickStates = hostSummary.getQuickStats();
				cpuUsedSize = quickStates.getOverallCpuUsage();
				//cpuUsedPercent = 100.0*cpuUsedSize/cpuTotalSize ; 
				memUsedSize = quickStates.getOverallMemoryUsage();
				JsonHostInfo.append(String.format("{ \"CPU Total By Mhz\":%s, \"CPU Used By Mhz\":%s, \"Memory Size By B\":%s, \"Memory Used By Mb\":%s }", 
						cpuTotalSize,cpuUsedSize,memTotalSize,memUsedSize));
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
	
	public String run() throws KeyManagementException, NoSuchAlgorithmException, RuntimeFaultFaultMsg, InvalidLoginFaultMsg, InvalidLocaleFaultMsg, InvalidPropertyFaultMsg{
		 if (!VCClientSession.IsConnected()) {
	            VCClientSession.Connect();
	     }
		 validateTheInput();
		 return getInfo();
	}
}
