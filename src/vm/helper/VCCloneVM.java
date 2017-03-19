package vm.helper;

import com.vmware.vim25.*;

import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by huxia on 2017/3/14.
 * 完成于当天晚上 BY 胡箫
 */
public class VCCloneVM extends VCTaskBase {
    /**
     * @param datacenterName datacenter的名字
     * @param vmPathName     虚拟机的清单路径
     * @param cloneName      克隆出虚拟机的名称
     * @功能描述 从现有的虚拟机创建出一个模板，并且创建这个模板的多个克隆实例到目标datacenter中
     */
    private static void CloneVM(String datacenterName, String vmPathName, String cloneName) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, NoSuchMethodException, IllegalAccessException, InvocationTargetException, TaskInProgressFaultMsg, InvalidDatastoreFaultMsg, InsufficientResourcesFaultFaultMsg, FileFaultFaultMsg, VmConfigFaultFaultMsg, InvalidStateFaultMsg, MigrationFaultFaultMsg, CustomizationFaultFaultMsg, InvalidCollectorVersionFaultMsg {
        init();

        // 找到数据中心的对象引用
        ManagedObjectReference datacenterRef = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), datacenterName);
        if (datacenterRef == null) {
            System.out.printf("The specified datacenter [ %s ]is not found %n", datacenterName);
            return;
        }

        // 找到这个虚拟机的目录
        ManagedObjectReference vmFolderRef = (ManagedObjectReference) getDynamicProperty(datacenterRef, "vmFolder");
        if (vmFolderRef == null) {
            System.out.printf("The virtual machine is not found");
            return;
        }
        ManagedObjectReference vmRef = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), vmPathName);
        if (vmRef == null) {
            System.out.printf("The VMPath specified [ %s ] is not found %n", vmPathName);
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

        System.out.printf("Cloning Virtual Machine [%s] to clone name [%s] %n", vmPathName.substring(vmPathName.lastIndexOf("/") + 1), cloneName);
        ManagedObjectReference cloneTask = vimPort.cloneVMTask(vmRef, vmFolderRef, cloneName, cloneSpec);
        if (getTaskResultAfterDone(cloneTask)) {
            System.out.printf("Successfully cloned Virtual Machine [%s] to clone name [%s] %n", vmPathName.substring(vmPathName.lastIndexOf("/") + 1), cloneName);
        } else {
            System.out.printf("Failure Cloning Virtual Machine [%s] to clone name [%s] %n", vmPathName.substring(vmPathName.lastIndexOf("/") + 1), cloneName);
        }
    }

    public static void run(String datacenterName, String vmPathName, String cloneName) throws InvalidLoginFaultMsg, NoSuchAlgorithmException, RuntimeFaultFaultMsg, InvalidLocaleFaultMsg, KeyManagementException, InsufficientResourcesFaultFaultMsg, InvocationTargetException, NoSuchMethodException, TaskInProgressFaultMsg, InvalidStateFaultMsg, IllegalAccessException, CustomizationFaultFaultMsg, FileFaultFaultMsg, MigrationFaultFaultMsg, InvalidPropertyFaultMsg, InvalidDatastoreFaultMsg, VmConfigFaultFaultMsg, InvalidCollectorVersionFaultMsg {
        if (!VCClientSession.IsConnected()) {
            VCClientSession.Connect();
        }

        CloneVM(datacenterName, vmPathName, cloneName);
    }
}
