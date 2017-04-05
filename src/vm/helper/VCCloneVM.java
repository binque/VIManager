package vm.helper;

import com.vmware.vim25.*;
import net.sf.json.JSONObject;

/**
 * Created by huxia on 2017/3/14.
 * 完成于当天晚上 BY 胡箫
 */
public class VCCloneVM extends VCTaskBase {
    /**
     * @param datacenterName datacenter的名字
     * @param vmPathName     虚拟机的清单路径
     * @param cloneName      克隆出虚拟机的名称
     * @param adminID        虚拟机管理员名称，该信息储存在Annotation中
     * @功能描述 从现有的虚拟机创建出一个模板，并且创建这个模板的多个克隆实例到目标datacenter中
     */
    private void CloneVM(String datacenterName, String vmPathName, String cloneName, String adminID, String studentID, String cpuNum, String memoryMb, String diskSizeMb, String diskMode) throws Exception {
        // 找到数据中心的对象引用
        ManagedObjectReference datacenterRef = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), datacenterName);
        if (datacenterRef == null) {
            logger.error(String.format("The specified datacenter [ %s ]is not found %n", datacenterName));
            return;
        }

        // 找到这个虚拟机的目录
        ManagedObjectReference vmFolderRef = (ManagedObjectReference) getDynamicProperty(datacenterRef, "vmFolder");
        if (vmFolderRef == null) {
            logger.error("The virtual machine is not found");
            return;
        }
        ManagedObjectReference vmRef = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), vmPathName);
        if (vmRef == null) {
            logger.error(String.format("The VMPath specified [ %s ] is not found %n", vmPathName));
            return;
        }

        // 对克隆机器进行配置，cloneSpec标识配置信息
        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        cloneSpec.setLocation(relocateSpec);
        // 标识克隆完成后不开机
        cloneSpec.setPowerOn(false);
        // 标识克隆完成后不置成模板
        cloneSpec.setTemplate(false);
        // 表示克隆机器的管理员ID
        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        JSONObject externalInfo = new JSONObject();
        externalInfo.put("adminID", adminID);
        externalInfo.put("studentID", studentID);
        configSpec.setAnnotation(externalInfo.toString());
        // 标识cpu数目
        if (cpuNum != null && !cpuNum.isEmpty()) {
            try {
                configSpec.setNumCoresPerSocket(Integer.parseInt(cpuNum));
                configSpec.setNumCPUs(Integer.parseInt(cpuNum));

            } catch (NumberFormatException e) {
                logger.error("Cpu Number [ " + cpuNum + " ] Must be a Numerical Value.");
            }
        }
        // 标识内存大小
        if (memoryMb != null && !memoryMb.isEmpty()) {
            try {
                configSpec.setMemoryMB(Long.parseLong(memoryMb));

            } catch (NumberFormatException e) {
                logger.error("Memory Size [ " + memoryMb + " ] Must be a Numerical Value.");
            }
        }
        // 标识磁盘模式和大小
        try {
            if (diskMode == null || diskMode.isEmpty()) {
                diskMode = "persistent";
            }
            for (VirtualDeviceConfigSpec deviceSpec : configSpec.getDeviceChange()) {
                if (deviceSpec.getDevice() instanceof VirtualDisk) {
                    deviceSpec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);
                    ((VirtualDisk) deviceSpec.getDevice()).setCapacityInKB(Integer.parseInt(diskSizeMb) * 1024);
                    if (deviceSpec.getDevice().getBacking() instanceof VirtualDiskFlatVer2BackingInfo) {
                        ((VirtualDiskFlatVer2BackingInfo) deviceSpec.getDevice().getBacking()).setDiskMode(diskMode);
                    }
                    if (deviceSpec.getDevice().getBacking() instanceof VirtualDiskFlatVer1BackingInfo) {
                        ((VirtualDiskFlatVer1BackingInfo) deviceSpec.getDevice().getBacking()).setDiskMode(diskMode);
                    }
                }
            }
        } catch (NumberFormatException e) {
            logger.error("Disk Size [ " + diskSizeMb + " ] Must be a Numerical Value.");
        }
        cloneSpec.setConfig(configSpec);

        logger.info(String.format("Cloning Virtual Machine [%s] to clone name [%s] %n", vmPathName.substring(vmPathName.lastIndexOf("/") + 1), cloneName));
        ManagedObjectReference cloneTask = vimPort.cloneVMTask(vmRef, vmFolderRef, cloneName, cloneSpec);
        if (getTaskResultAfterDone(cloneTask)) {
            logger.info(String.format("Successfully cloned Virtual Machine [%s] to clone name [%s] %n", vmPathName.substring(vmPathName.lastIndexOf("/") + 1), cloneName));
        } else {
            logger.error(String.format("Failure Cloning Virtual Machine [%s] to clone name [%s] %n", vmPathName.substring(vmPathName.lastIndexOf("/") + 1), cloneName));
        }
    }

    public void run(String datacenterName, String vmPathName, String cloneName, String adminID, String studentID, String cpuNum, String memoryMb, String diskSizeMb, String diskMode) throws Exception {
        try {
            init();
            CloneVM(datacenterName, vmPathName, cloneName, adminID, studentID, cpuNum, memoryMb, diskSizeMb, diskMode);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            vcClientSession.Disconnect();
        }
    }
}
