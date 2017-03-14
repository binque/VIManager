/*
  Created by huxiaon 2017/3/13.
 */
 package vm.manager;

import javax.jws.WebService;

/**
 * @功能描述：web服务中暴露的接口，调用下列接口进行vsphere管理
 */
@WebService
public class VCManager {
    /**
     * @功能描述：编辑虚拟机配置
     * @param VMID          虚拟机的名字
     * @param CPU           给虚拟机分配的cpu核心数目
     * @param RAM           给虚拟机分配的内存大小
     * @param Disk          给虚拟机分配的磁盘空间大小
     * @param NetLimit      网络限制
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     */
    public String ChangeConfig(String VMID, String CPU, String RAM, String Disk, String NetLimit) {
        return null;
    }

    /**
     * @功能描述：对虚拟机进行的基本操作
     * @param VMID          虚拟机的名字
     * @param Op            操作的名字，可以使poweron、shutdown、reboot等
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     */
    public String BasicOps(String VMID, String Op) {
        return null;
    }

    /**
     * @功能描述：从模板创建一个虚拟机
     * @param templateID    模板名称
     * @param VMID          虚拟机名称
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     */
    public String CreateFromTemplate(String templateID, String VMID) {
        return null;
    }

    /**
     * @功能描述：到处虚拟机到目标目录
     * @param VMID          虚拟机名称
     * @param targetDir     目标目录
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     */
    public String ExportVM(String VMID, String targetDir) {
        return null;
    }

    /**
     * @功能描述：获得虚拟机信息
     * @param VMID          虚拟机名称
     * @return 如果成功则返回表示虚拟机信息的字符串，否则返回表示错误信息的字符串
     */
    public String GetVMInfor(String VMID) {
        return null;
    }

    /**
     *
     * @param VMID              虚拟机名称
     * @param PerfUnit          perf
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     */
    public String GetPerf(String VMID, String PerfUnit) {
        return null;
    }

    /**
     * @功能描述：虚拟机快照
     * @param VMID              虚拟机名称
     * @param op                操作
     * @param snapshotname      快照名称
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     */
    public String SnapShotOps(String VMID, String op, String snapshotname) {
        return null;
    }

    /**
     * @功能描述：执行虚拟机上的例程
     * @param VMID      虚拟机名称
     * @param fileDir   例程路径
     * @param args      执行参数
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     */
    public String RunGuestProgram(String VMID, String fileDir, String args) {
        return null;
    }

    /**
     * @功能描述：将文件上传到虚拟机中
     * @param VMID              虚拟机名称
     * @param filePathLocal     源位置（本机路径）
     * @param filePathInGuest   目标位置（客户集中的路径）
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     */
    public String uploadFile(String VMID, String filePathLocal, String filePathInGuest) {
        return null;
    }
}
