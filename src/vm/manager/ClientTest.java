package vm.manager;

import vm.helper.VCClientSession;
import vm.helper.VCCloneVM;

/*
 * Created by huxia on 2017/3/13.
 */
public class ClientTest {
    public static void main(String[] args) {
        // 连接vcenter与中断连接的测试代码
        try{
            VCClientSession.Connect();
            VCCloneVM.run("Datacenter", "Datacenter/vm/Temptest", "CloneTest");
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
