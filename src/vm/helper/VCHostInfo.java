package vm.helper;

import com.vmware.vim25.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.Map;

/*
 * hostName 为主机的名字
 * getInfo方法 是该类中的主要方法
 */
public class VCHostInfo extends VCTaskBase {

    //获取主机的cpu、mem的信息通过HostListSummary类
    private String getHostInfo() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        JSONArray JsonHostInfo = new JSONArray();

        String[] infoName = {"summary"};
        ManagedObjectReference container = serviceContent.getRootFolder();
        Map<ManagedObjectReference, Map<String, Object>> hostMap = VCHelper.inContainerByType(serviceContent, vimPort, container, "HostSystem", infoName, new RetrieveOptions());
        for (ManagedObjectReference host : hostMap.keySet()) {
            Map<String, Object> m = hostMap.get(host);
            //cpu 单位为Mhz 可能需要更改为Ghz
            int cpuTotalSize;
            int cpuUsedSize = -1;
            //double cpuUsedPercent;

            long memTotalSize; //单位为b
            int memUsedSize = -1; //单位为Mb
            //double memUsedPercent;

            HostListSummary hostSummary = (HostListSummary) m.get(infoName[0]);
            HostHardwareSummary hardwareSummary = hostSummary.getHardware();
            cpuTotalSize = hardwareSummary.getCpuMhz() * hardwareSummary.getNumCpuCores();
            memTotalSize = hardwareSummary.getMemorySize();
            HostListSummaryQuickStats quickStates = hostSummary.getQuickStats();
            if (hostSummary.getRuntime().getConnectionState().value().equalsIgnoreCase("connected")) {
                cpuUsedSize = quickStates.getOverallCpuUsage();
                //cpuUsedPercent = 100.0*cpuUsedSize/cpuTotalSize ;
                memUsedSize = quickStates.getOverallMemoryUsage();
            }

            String[] vmProp = {"name"};
            Map<ManagedObjectReference, Map<String, Object>> vmlist = VCHelper.inContainerByType(serviceContent, vimPort, host, "VirtualMachine", vmProp, new RetrieveOptions());
            JSONArray Jsonvmlist = new JSONArray();
            if (vmlist != null) {
                for (Map<String, Object> val : vmlist.values()) {
                    Jsonvmlist.add(val.get(vmProp[0]));
                }
            }

            JSONObject jo = new JSONObject();
            jo.put("Host Name", hostSummary.getConfig().getName());
            jo.put("Is Connected", hostSummary.getRuntime().getConnectionState().value());
            jo.put("Run Time State", hostSummary.getOverallStatus().value());
            jo.put("CPU Total By Mhz", cpuTotalSize);
            jo.put("CPU Used By Mhz", cpuUsedSize);
            jo.put("Memory Size By B", memTotalSize);
            jo.put("Memory Used By Mb", memUsedSize);
            jo.put("Virtual Machine List", Jsonvmlist);

            JsonHostInfo.add(jo);

            //logger.info("CPU total:"+cpuTotalSize+"CPU ued "+quickStates.getOverallCpuUsage());
            //logger.info("cpu used percent "+cpuUsedPercent);
            //logger.info("mem size :"+memTotalSize+" used :"+memUsedSize);

            //info = quickStates.getOverallMemoryUsage().toString();
        }

        return JsonHostInfo.toString();
    }

    public String run() throws Exception {
        try {
            init();
            return getHostInfo();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            vcClientSession.Disconnect();
        }

    }
}
