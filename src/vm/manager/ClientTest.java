package vm.manager;

import vm.helper.VCGetVMList;
import vm.helper.VCHostInfo;

/*
 * Created by huxia on 2017/3/13.
 */
public class ClientTest {
    public static void main(String[] args) {
        // 连接vcenter与中断连接的测试代码
        try {
            // 克隆虚拟机的测试代码
            // VCCloneVM.run("Datacenter", "Datacenter/vm/Temptest", "CloneTest");

            // 删除被管实体的测试代码
            // VCDeleteEntity.run("Temptest");

            // 重新配置被管实体的测试代码
            // VCConfigVM.run("CloneTest", "update", "memory", "normal", "", "");

            //VCCloneVM.run("Datacenter", "Datacenter/vm/ServiceTest", "CloneTest44", "admin", "student", "1", "1024", "4096", "persistent");
            //VCDeleteEntity.run("ServiceTest");
            //VCConfigVM.run("ServiceTest", "2", "1024", null, null, null);
            // 获取虚拟机名称列表
            String vmList = new VCGetVMList().run("Datacenter", null, null);
            System.out.println(vmList);

            // 获取主机信息
            String hostInfo = new VCHostInfo().run();
            System.out.println(hostInfo);

            // 获取许可证信息
            //String licenseInfo = VCLicensesInfo.run();
            //System.out.println(licenseInfo);

            // 获取存储信息
            //String storageInfo = VCStorageInfo.run();
            //System.out.println(storageInfo);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
}
