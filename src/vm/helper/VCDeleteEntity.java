package vm.helper;

import com.vmware.vim25.*;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by huxia on 2017/3/15.
 */
public class VCDeleteEntity extends VCTaskBase {
    /**
     * @param entityName 要删除的实体的名称，如Datacenter\myFolder\myVirtualMachine
     * @功能描述 这个函数用来从清单中删除一个特定的实体，可以是虚拟机、数据中心、目录或者集群计算资源等。
     */
    private static void deleteManagedObject(String entityName) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, VimFaultFaultMsg, InvalidCollectorVersionFaultMsg {
        init();

        ManagedObjectReference moref = VCHelper.vmByVmname(entityName, serviceContent.getPropertyCollector());
        if (moref == null) {
            System.out.printf("Managed entity cannot be found by name [ %s ]", entityName);
            return;
        } else {
            ManagedObjectReference taskmor = vimPort.destroyTask(moref);
            if (getTaskResultAfterDone(taskmor)) {
                System.out.printf("Successful delete of Managed Entity Name - [ %s ]"
                        + " and Entity Type - [ %s ]%n", entityName, moref.getType());
            }
        }
    }

    public static void run(String vmyname) throws InvalidLoginFaultMsg, NoSuchAlgorithmException, RuntimeFaultFaultMsg, InvalidLocaleFaultMsg, KeyManagementException, InvalidCollectorVersionFaultMsg, InvalidPropertyFaultMsg, VimFaultFaultMsg {
        if (!VCClientSession.IsConnected()) {
            VCClientSession.Connect();
        }

        deleteManagedObject(vmyname);
    }
}
