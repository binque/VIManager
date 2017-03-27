/*
  Created by huxiaon 2017/3/13.
 */
 package vm.manager;

import vm.helper.*;

import javax.jws.WebMethod;
import javax.jws.WebService;

import static vm.helper.VCClientSession.Disconnect;

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
        String retVal = "";

        try {
            if (VMID != null && !VMID.isEmpty() && CPU.equals("low") || CPU.equals("normal") || CPU.equals("high")) {
                VCConfigVM.run(VMID, "update", "cpu", CPU, "", "");
            }
            if (VMID != null && !VMID.isEmpty() && RAM.equals("low") || RAM.equals("normal") || RAM.equals("high")) {
                VCConfigVM.run(VMID, "update", "memory", RAM, "", "");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            retVal =  "error :" + e.getMessage();
        } finally {
            retVal += disconnect();
        }
        return retVal;
    }

    /**
     * @param VMID 虚拟机的名字
     * @param Op   操作的名字，可以使poweron、shutdown、reboot等
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 对虚拟机进行的基本操作
     */
    @WebMethod
    public String BasicOps(String VMID, String Op) {
        String retVal = "";
        if (VMID != null && Op != null && (!(VMID.isEmpty() || Op.isEmpty()))) {
            try {
                switch (Op.toLowerCase()) {
                    case "delete": {
                        VCDeleteEntity.run(VMID);
                        Disconnect();
                        retVal = "success finished.";
                    }
                    case "poweron": {
                        VCVMPower vm = new VCVMPower();
                        vm.setVmName(VMID);
                        vm.setOperation("poweron");
                        vm.run();
                        Disconnect();
                        retVal = "success finished.";
                    }
                    case "poweroff": {
                        VCVMPower vm = new VCVMPower();
                        vm.setVmName(VMID);
                        vm.setOperation("poweroff");
                        vm.run();
                        Disconnect();
                        retVal = "success finished.";
                    }
                    case "reboot": {
                        VCVMPower vm = new VCVMPower();
                        vm.setVmName(VMID);
                        vm.setOperation("reboot");
                        vm.run();
                        Disconnect();
                        retVal = "success finished.";
                    }
                    default:
                        retVal = "error :parmeter cannot resolved.";
                }
            } catch (Throwable e) {
                e.printStackTrace();
                retVal += "error :" + e.getMessage();
            }finally {
                retVal += disconnect();
            }
        } else {
            retVal = "error :parameter cannot be null.";
        }
        return retVal;
    }

    /**
     * @param templateID 模板名称
     * @param VMID       虚拟机名称
     * @return 如果任务执行完成（不意味着虚拟机创建完成），返回以success开始的字符串；否则返回以error开始，表示错误信息的字符串
     * @功能描述 从模板创建一个虚拟机
     */
    @WebMethod
    public String CreateFromTemplate(String templateID, String VMID, String adminID) {
        String retVal = "";
        if (templateID != null && !templateID.isEmpty() && VMID != null && !VMID.isEmpty()) {
            try {
                VCCloneVM.run("Datacenter", "Datacenter/vm/" + templateID, VMID, adminID);
                retVal = "success finished.";
            } catch (Throwable e) {
                e.printStackTrace();
                retVal = "error :" + e.getMessage();
            } finally {
                retVal += disconnect();
            }
        } else {
            retVal = "error :parameter cannot be null.";
        }
        return retVal;
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
    public String UploadFile(String VMID, String filePathLocal, String filePathInGuest) {
        return null;
    }

    /**
     * @功能描述 返回包含虚拟机信息的字符串，以json格式给出，包含内容有：虚拟机名称、是否是模板、电源状态、运行状态、磁盘大小（B）、内存大小（MB）、CPU数目、操作系统全名。
     * 其中运行状态以四个颜色表示：gray（不可知）、green（虚拟机正常）、red（该虚拟机出现了问题）、yellow（虚拟机可能出现了问题）
     * @return 虚拟机名称列表，以json的格式，包含虚拟机名称、是否是模板、电源状态、运行状态、磁盘大小（B）、内存大小（MB）、CPU数目、操作系统全名
     */
    @WebMethod
    public String GetVMList(String adminID) {
        String retVal = "";
        try {
            retVal = VCGetVMList.run("Datacenter", adminID);
        } catch (Throwable e) {
            e.printStackTrace();
            retVal = "error :" + e.getMessage();
        } finally {
            String temp = disconnect();
            if (temp.startsWith("error")) {
                retVal += temp;
            }
        }
        return retVal;
    }

    private String disconnect() {
        String retVal = "";
        try {
            VCClientSession.Disconnect();
            retVal = "success disconnect.";
        } catch (Throwable e) {
            e.printStackTrace();
            retVal += "error :" + e.getMessage();
        }
        return  retVal;
    }
}
