package vm.manager;

import vm.helper.VCClientSession;
import vm.helper.VCGetVMList;

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

            // 获取虚拟机名称列表
            String vmList = VCGetVMList.run("Datacenter", null);
            System.out.printf(vmList);
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
