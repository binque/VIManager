package vm.helper;

import com.vmware.vim25.*;

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
        props[0] = "summary";

        Map<ManagedObjectReference, Map<String, Object>> vmList = VCHelper.inContainerByType(vmFolder, "VirtualMachine", props, new RetrieveOptions());

        StringBuilder JsonVmList = new StringBuilder();
        JsonVmList.append('[');

        for (Map<String, Object> val : vmList.values()) {
            VirtualMachineSummary virtualMachineSummary = (VirtualMachineSummary) (val.get(props[0]));
            if (adminID == null || virtualMachineSummary.getConfig().getManagedBy().getExtensionKey().equalsIgnoreCase(adminID)) {
                String runtimeState = virtualMachineSummary.getOverallStatus().value();

                JsonVmList.append(String.format("{ \"vmName\":%s,\"isTemplate\":%s,\"powerState\":%s,\"runTimeState\":%s,\"diskByB\":%s,\"memoryByMb\":%s,\"CPUByNum\":%s,\"guestFullName\":%s }, ",
                        virtualMachineSummary.getConfig().getName(), virtualMachineSummary.getConfig().isTemplate(), virtualMachineSummary.getRuntime().getPowerState().value(), runtimeState,
                        virtualMachineSummary.getStorage().getCommitted(), virtualMachineSummary.getConfig().getMemorySizeMB(), virtualMachineSummary.getConfig().getNumCpu(), virtualMachineSummary.getConfig().getGuestFullName()));
            }
        }
        JsonVmList.deleteCharAt(JsonVmList.length() - 2);
        JsonVmList.append("]");

        return JsonVmList.toString();
    }

    public static String run(String datacenterName, String adminID) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidLoginFaultMsg, NoSuchAlgorithmException, InvalidLocaleFaultMsg, KeyManagementException {
        if (!VCClientSession.IsConnected()) {
            VCClientSession.Connect();
        }
        init();

        // 找到数据中心
        ManagedObjectReference datacenter = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), datacenterName);

        return getVmNamesByVmFolder(datacenter, adminID);
    }
}
