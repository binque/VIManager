package vm.helper;

import com.vmware.vim25.*;

/**
 * Created by huxia on 2017/3/14.
 * 完成于当天晚上 BY 胡箫
 */
public class VCCloneVM extends VCTaskBase {
    /**
     * @param datacenterName datacenter的名字
     * @param vmPathName     虚拟机的清单路径
     * @param cloneName      克隆出虚拟机的名称
     * @param adminID 虚拟机管理员名称，该信息储存在Annotation中
     * @功能描述 从现有的虚拟机创建出一个模板，并且创建这个模板的多个克隆实例到目标datacenter中
     */
    private static void CloneVM(String datacenterName, String vmPathName, String cloneName, String adminID) throws Exception {
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
        if (adminID == null) {
            configSpec.setAnnotation("default");
        } else {
            configSpec.setAnnotation(adminID);
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

    public static void run(String datacenterName, String vmPathName, String cloneName, String adminID, String cpuNum, String memoryMb, String diskSizeMb, String diskMode) throws Exception {
        try {
            init();
            CloneVM(datacenterName, vmPathName, cloneName, adminID);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            VCClientSession.Disconnect();
        }
    }
}
