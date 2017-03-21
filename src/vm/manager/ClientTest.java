package vm.manager;

import com.vmware.vim25.ManagedObjectReference;
import vm.helper.VCClientSession;
import vm.helper.VCHelper;

import java.util.Map;

/*
 * Created by huxia on 2017/3/13.
 */
public class ClientTest {
    public static void main(String[] args) {
        // 连接vcenter与中断连接的测试代码
        try{
            VCClientSession.Connect();
            // 克隆虚拟机的测试代码
            // VCCloneVM.run("Datacenter", "Datacenter/vm/Temptest", "CloneTest");

            // 删除被管实体的测试代码
            // VCDeleteEntity.run("Temptest");

            // 重新配置被管实体的测试代码
            // VCConfigVM.run("CloneTest", "update", "memory", "normal", "", "");

            Map map = VCHelper.inContainerByType(VCClientSession.getServiceContent().getRootFolder(), "VirtualMachine");
            System.out.printf("map size is %s\n", map.size());
            ManagedObjectReference vm = (ManagedObjectReference) map.get("PowerOnTest");
            System.out.printf("vm type is %s, its value is %s.\n", vm.getType(), vm.getValue());
            System.out.printf("its class is %s", vm.getClass());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                VCClientSession.Disconnect();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
