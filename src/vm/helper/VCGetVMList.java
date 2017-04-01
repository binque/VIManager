package vm.helper;

import com.vmware.vim25.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by huxia on 2017/3/22.
 * Completed By Huxiao
 */
public class VCGetVMList extends VCTaskBase {
    /**
     * @param vmFolder 目录
     * @return 虚拟机名称列表，以json的格式，包含虚拟机名称、是否是模板、电源状态、运行状态、磁盘大小（B）、内存大小（MB）、CPU数目、操作系统全名
     * @功能描述 根据目录获得虚拟机名称列表
     */
    private static String getVmNamesByVmFolder(ManagedObjectReference vmFolder, String adminID) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidLoginFaultMsg, NoSuchAlgorithmException, InvalidLocaleFaultMsg, KeyManagementException {

        String[] props = new String[1];
        props[0] = "name";
        Map<ManagedObjectReference, Map<String, Object>> hostList = VCHelper.inContainerByType(vmFolder, "HostSystem", props, new RetrieveOptions());
        props[0] = "summary";

        JSONArray JsonVmList = new JSONArray();

        for (ManagedObjectReference host : hostList.keySet()) {
            if (host != null) {
                String hostName = (String) hostList.get(host).get("name");
                Map<ManagedObjectReference, Map<String, Object>> vmlist = VCHelper.inContainerByType(host, "VirtualMachine", props, new RetrieveOptions());
                if (vmlist != null) {
                    for (Map<String, Object> val : vmlist.values()) {
                        VirtualMachineSummary virtualMachineSummary = (VirtualMachineSummary) (val.get(props[0]));
                        if (adminID == null || virtualMachineSummary.getConfig().getAnnotation().equals(adminID)) {
                            JSONObject jo = new JSONObject();
                            jo.put("vmName", virtualMachineSummary.getConfig().getName());
                            jo.put("isTemplate", virtualMachineSummary.getConfig().isTemplate());
                            jo.put("powerState", virtualMachineSummary.getRuntime().getPowerState().value());
                            jo.put("runTimeState", virtualMachineSummary.getOverallStatus().value());
                            jo.put("diskByB", virtualMachineSummary.getStorage().getCommitted());
                            jo.put("memoryByMb", virtualMachineSummary.getConfig().getMemorySizeMB());
                            jo.put("CPUByNum", virtualMachineSummary.getConfig().getNumCpu());
                            jo.put("guestFullName", virtualMachineSummary.getConfig().getGuestFullName());
                            jo.put("adminID", virtualMachineSummary.getConfig().getAnnotation());
                            jo.put("hostName", hostName);
                            JsonVmList.add(jo);
                        }
                    }
                }
            }
        }
        return JsonVmList.toString();

    }

    public static String run(String datacenterName, String adminID) throws Exception {
        try {
            init();

            // 找到数据中心
            ManagedObjectReference datacenter = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), datacenterName);

            return getVmNamesByVmFolder(datacenter, adminID);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            VCClientSession.Disconnect();
        }
    }
}
