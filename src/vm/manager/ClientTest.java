package vm.manager;

import vm.helper.ClientSession;

/*
 * Created by huxia on 2017/3/13.
 */
public class ClientTest {
    public static void main(String[] args) {
        // 连接vcenter与中断连接的测试代码
        try{
            ClientSession.Connect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ClientSession.Disconnect();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
