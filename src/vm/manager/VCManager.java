/*
  Created by huxiaon 2017/3/13.
 */
 package vm.manager;

import vm.helper.*;

import javax.jws.WebMethod;
import javax.jws.WebService;

import static vm.helper.VCClientSession.*;

/**
 * @功能描述 web服务中暴露的接口，调用下列接口进行vsphere管理
 */
@WebService
public class VCManager {
    /**     * @param VMID     虚拟机的名字
     * @param CPU      给虚拟机分配的cpu核心数目
     * @param RAM      给虚拟机分配的内存大小
     * @param Disk     给虚拟机分配的磁盘空间大小
     * @param NetLimit 网络限制
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 编辑虚拟机配置
     */
    @WebMethod
    public String ChangeConfig(String VMID, String CPU, String RAM, String Disk, String NetLimit) {
        try {
            if (VMID != null && !VMID.isEmpty() && (CPU == "low" || CPU == "normal" || CPU == "high")) {
                VCConfigVM.run(VMID, "update", "cpu", CPU, "", "");
            }
            if (VMID != null && !VMID.isEmpty() && (RAM == "low" || RAM == "normal" || RAM == "high")) {
                VCConfigVM.run(VMID, "update", "memory", RAM, "", "");
            }

        } catch (Throwable e) {
            e.printStackTrace();
            return "error :" + e.getMessage();
        } finally {
            try {
                Disconnect();
                return "success finished.";
            } catch (Throwable e) {
                e.printStackTrace();
                return "error :" + e.getMessage();
            }
        }
    }

    /**
     * @param VMID 虚拟机的名字
     * @param Op   操作的名字，可以使poweron、shutdown、reboot等
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 对虚拟机进行的基本操作
     */
    @WebMethod
    public String BasicOps(String VMID, String Op) {
        if (VMID != null && Op != null && (!(VMID.isEmpty() || Op.isEmpty()))) {
            switch (Op.toLowerCase()) {
                case "delete":
                    try {
                        VCDeleteEntity.run(VMID);
                        Disconnect();
                        return "success finished.";
                    } catch (Throwable e) {
                        e.printStackTrace();
                        return "error :" + e.getMessage();
                    }
                case "poweron":
                    try {
                        VCVMPower vm = new VCVMPower();
                        vm.setVmName(VMID);
                        vm.setOperation("poweron");
                        vm.run();
                        Disconnect();
                        return "success finished.";
                    } catch (Throwable e) {
                        e.printStackTrace();
                        return "error :" + e.getMessage();
                    }
                case "poweroff":
                    try {
                        VCVMPower vm = new VCVMPower();
                        vm.setVmName(VMID);
                        vm.setOperation("poweroff");
                        vm.run();
                        Disconnect();
                        return "success finished.";
                    } catch (Throwable e) {
                        e.printStackTrace();
                        return "error :" + e.getMessage();
                    }

                case "reboot":
                    try {
                        VCVMPower vm = new VCVMPower();
                        vm.setVmName(VMID);
                        vm.setOperation("reboot");
                        vm.run();
                        Disconnect();
                        return "success finished.";
                    } catch (Throwable e) {
                        e.printStackTrace();
                        return "error :" + e.getMessage();
                    }
                default:
                    return "error :parmeter cannot resolved.";
            }
        } else {
            return "parameter cannot be null.";
        }
    }

    /**
     * @param templateID 模板名称
     * @param VMID       虚拟机名称
     * @return 如果任务执行完成（不意味着虚拟机创建完成），返回以success开始的字符串；否则返回以error开始，表示错误信息的字符串
     * @功能描述 从模板创建一个虚拟机
     */
    @WebMethod
    public String CreateFromTemplate(String templateID, String VMID) {
        if (templateID != null && !templateID.isEmpty() && VMID != null && !VMID.isEmpty()) {
            try {
                VCCloneVM.run("Datacenter", "Datacenter/vm/" + templateID, VMID);
                return "success finished.";
            } catch (Throwable e) {
                e.printStackTrace();
                return "error :" + e.getMessage();
            } finally {
                try {
                    Disconnect();
                    return "success finished.";
                } catch (Throwable e) {
                    e.printStackTrace();
                    return "error :" + e.getMessage();
                }
            }
        } else {
            return "parameter cannot be null.";
        }
    }

    /**
     * @param VMID      虚拟机名称
     * @param targetDir 目标目录
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 到处虚拟机到目标目录
     */
    @WebMethod
    public String ExportVM(String VMID, String targetDir) {
        return null;
    }

    /**
     * @param VMID 虚拟机名称
     * @return 如果成功则返回表示虚拟机信息的字符串，否则返回表示错误信息的字符串
     * @功能描述 获得虚拟机信息
     */
    @WebMethod
    public String GetVMInfor(String VMID) {
        return null;
    }

    /**
     * @param VMID     虚拟机名称
     * @param PerfUnit perf
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     */
    @WebMethod
    public String GetPerf(String VMID, String PerfUnit) {
        return null;
    }

    /**
     * @param VMID         虚拟机名称
     * @param op           操作
     * @param snapshotname 快照名称
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 虚拟机快照
     */
    @WebMethod
    public String SnapShotOps(String VMID, String op, String snapshotname) {
        return null;
    }

    /**
     * @param VMID    虚拟机名称
     * @param fileDir 例程路径
     * @param args    执行参数
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 执行虚拟机上的例程
     */
    @WebMethod
    public String RunGuestProgram(String VMID, String fileDir, String args) {
        return null;
    }

    /**
     * @param VMID            虚拟机名称
     * @param filePathLocal   源位置（本机路径）
     * @param filePathInGuest 目标位置（客户集中的路径）
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 将文件上传到虚拟机中
     */
    @WebMethod
    public String uploadFile(String VMID, String filePathLocal, String filePathInGuest) {
        return null;
    }
}
