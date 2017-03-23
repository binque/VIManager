package vm.helper;

import com.vmware.vim25.*;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by huxia on 2017/3/22.
 */
public class VCGetVMList extends VCTaskBase {
    /**
     * @功能描述 根据目录获得虚拟机名称列表
     * @param vmFolder 目录
     * @return 虚拟机名称列表，以json的格式，包含虚拟机名称、是否是模板、电源状态、运行状态、磁盘大小（B）、内存大小（MB）、CPU数目、操作系统全名
     */
    private static String getVmNamesByVmFolder(ManagedObjectReference vmFolder) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        init();

        String[] props = new String[1];
        props[0] = "summary";

        Map<ManagedObjectReference, Map<String, Object>> vmList = VCHelper.inContainerByType(vmFolder, "VirtualMachine", props, new RetrieveOptions());
        Object[] keys = vmList.keySet().toArray();

        StringBuilder JsonVmList = new StringBuilder();
        JsonVmList.append('[');

        for (Object key : keys) {
            VirtualMachineSummary virtualMachineSummary = (VirtualMachineSummary) (vmList.get(key).get(props[0]));

            String runtimeState = virtualMachineSummary.getOverallStatus().value();

            JsonVmList.append(String.format("{ \"vmName\":%s,\"isTemplate\":%s,\"powerState\":%s,\"runTimeState\":%s,\"diskByB\":%s,\"memoryByMb\":%s,\"CPUByNum\":%s,\"guestFullName\":%s }, ",
                    virtualMachineSummary.getConfig().getName(), virtualMachineSummary.getConfig().isTemplate(), virtualMachineSummary.getRuntime().getPowerState().value(), runtimeState,
                    virtualMachineSummary.getStorage().getCommitted(), virtualMachineSummary.getConfig().getMemorySizeMB(), virtualMachineSummary.getConfig().getNumCpu(), virtualMachineSummary.getConfig().getGuestFullName()));
        }
        JsonVmList.deleteCharAt(JsonVmList.length() - 2);
        JsonVmList.append("]");

        return JsonVmList.toString();
    }

    public static String run(String datacenterName) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidLoginFaultMsg, NoSuchAlgorithmException, InvalidLocaleFaultMsg, KeyManagementException {
        if (!VCClientSession.IsConnected()) {
            VCClientSession.Connect();
        }

        // 找到数据中心
        ManagedObjectReference datacenter = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), datacenterName);

        return getVmNamesByVmFolder(datacenter);
    }
}
