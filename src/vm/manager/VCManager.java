/*
  Created by huxiaon 2017/3/13.
 */
 package vm.manager;

import vm.helper.*;

import javax.jws.WebMethod;
import javax.jws.WebService;

/**
 * @功能描述 web服务中暴露的接口，调用下列接口进行vsphere管理
 */
@WebService
public class VCManager {
    /**
     * @param vmName   虚拟机的名字
     * @param CPU      给虚拟机分配的cpu核心数目，可以是一个数字或者low(1)/normal(2)/high(4)
     * @param memory   给虚拟机分配的内存大小，可以是一个数字或者low(1024)/normal(2048)/high(4096)
     * @param diskname 给虚拟机增加或者移除的磁盘名称 虚拟磁盘模式(diskmode)、虚拟磁盘大小(disksize)
     * @param disksize 磁盘大小，如果指定了磁盘大小，则增加磁盘，如果未指定磁盘大小，则移除磁盘
     * @param diskmode 磁盘模式，可选persistent/independent_persistent/independent_nonpersistent/nonpersistent/undoable/append
     * @return 如果成功则返回空字符串，否则返回表示错误信息的字符串
     * @功能描述 编辑虚拟机配置
     */
    @WebMethod
    public String ChangeConfig(String vmName, String CPU, String memory, String diskname, String disksize, String diskmode) {
        String retVal;
        try {
            VCConfigVM.run(vmName, CPU, memory, diskname, disksize, diskmode);
            retVal = "success finished.";
        } catch (Throwable e) {
            retVal = "error :" + e.getMessage();
        }
        return retVal;
    }

    @WebMethod
    /*
     * @功能描述 获取主机信息列表
     */
    public String GetHostInfo() {
        String info;
        try {
            info = VCHostInfo.run();
        } catch (Exception e) {
            info = "error :" + e.getMessage();
        }
        return info;
    }

    @WebMethod
    /*
     * @功能描述 获取证书信息
     */
    public String GetLicenseInfo() {
        String info;
        try {
            info = VCLicensesInfo.run();
        } catch (Exception e) {
            info = "error :" + e.getMessage();
        }
        return info;
    }

    @WebMethod
    /*
     * @功能描述 获取存储信息
     */
    public String GetStorageInfo() {
        String info;
        try {
            info = VCStorageInfo.run();
        } catch (Exception e) {
            info = "error :" + e.getMessage();
        }
        return info;
    }

    /**
     * @param vmName 虚拟机的名字
     * @param Op     操作的名字，可以使poweron、shutdown、reboot等
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 对虚拟机进行的基本操作
     */
    @WebMethod
    public String BasicOps(String vmName, String Op) {
        String retVal = "";
        if (vmName != null && Op != null && (!(vmName.isEmpty() || Op.isEmpty()))) {
            try {
                switch (Op.toLowerCase()) {
                    case "delete": {
                        VCDeleteEntity.run(vmName);
                        retVal = "success finished.";
                    }
                    case "poweron": {
                        VCVMPower vm = new VCVMPower();
                        vm.setVmName(vmName);
                        vm.setOperation("poweron");
                        vm.run();
                        retVal = "success finished.";
                    }
                    case "poweroff": {
                        VCVMPower vm = new VCVMPower();
                        vm.setVmName(vmName);
                        vm.setOperation("poweroff");
                        vm.run();
                        retVal = "success finished.";
                    }
                    case "reboot": {
                        VCVMPower vm = new VCVMPower();
                        vm.setVmName(vmName);
                        vm.setOperation("reboot");
                        vm.run();
                        retVal = "success finished.";
                    }
                    default:
                        retVal = "error :parmeter cannot resolved.";
                }
            } catch (Throwable e) {
                retVal += "error :" + e.getMessage();
            }
        } else {
            retVal = "error :parameter cannot be null.";
        }
        return retVal;
    }

    /**
     * @param templateID 模板名称
     * @param vmName     虚拟机名称
     * @param adminID 虚拟机管理员名称，该信息储存在Annotation中
     * @param cpuNum 克隆虚拟机的cpu核心数目
     * @param memoryMb 克隆虚拟机的内存大小
     * @param diskSizeMb 克隆虚拟机的磁盘大小
     * @param diskmode 克隆虚拟机的磁盘模式，见配置虚拟机说明
     * @return 如果任务执行完成（不意味着虚拟机创建完成），返回以success开始的字符串；否则返回以error开始，表示错误信息的字符串
     * @功能描述 从模板创建一个虚拟机
     */
    @WebMethod
    public String CreateFromTemplate(String templateID, String vmName, String adminID, String cpuNum, String memoryMb, String diskSizeMb, String diskmode) {
        String retVal;
        if (templateID != null && !templateID.isEmpty() && vmName != null && !vmName.isEmpty()) {
            try {
                VCCloneVM.run("Datacenter", "Datacenter/vm/" + templateID, vmName, adminID, cpuNum, memoryMb, diskSizeMb, diskmode);
                retVal = "success finished.";
            } catch (Throwable e) {
                retVal = "error :" + e.getMessage();
            }
        } else {
            retVal = "error :parameter cannot be null.";
        }
        return retVal;
    }

    /**
     * @return 虚拟机名称列表，以json的格式，包含虚拟机名称、是否是模板、电源状态、运行状态、磁盘大小（B）、内存大小（MB）、CPU数目、操作系统全名
     * @功能描述 返回包含虚拟机信息的字符串，以json格式给出，包含内容有：虚拟机名称、是否是模板、电源状态、运行状态、磁盘大小（B）、内存大小（MB）、CPU数目、操作系统全名。
     * 其中运行状态以四个颜色表示：gray（不可知）、green（虚拟机正常）、red（该虚拟机出现了问题）、yellow（虚拟机可能出现了问题）
     */
    @WebMethod
    public String GetVMList(String adminID) {
        String retVal;
        try {
            retVal = VCGetVMList.run("Datacenter", adminID);
        } catch (Throwable e) {
            retVal = "error :" + e.getMessage();
        }
        return retVal;
    }

    /**
     * @param vmName    虚拟机名称
     * @param targetDir 目标目录
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 到处虚拟机到目标目录
     */
    @WebMethod
    public String ExportVM(String vmName, String targetDir) {
        return null;
    }

    /**
     * @param vmName 虚拟机名称
     * @return 如果成功则返回表示虚拟机信息的字符串，否则返回表示错误信息的字符串
     * @功能描述 获得虚拟机信息
     */
    @WebMethod
    public String GetVMInfor(String vmName) {
        return null;
    }

    /**
     * @param vmName       虚拟机名称
     * @param op           操作
     * @param snapshotname 快照名称
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 虚拟机快照
     */
    @WebMethod
    public String SnapShotOps(String vmName, String op, String snapshotname) {
        return null;
    }

    /**
     * @param vmName  虚拟机名称
     * @param fileDir 例程路径
     * @param args    执行参数
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 执行虚拟机上的例程
     */
    @WebMethod
    public String RunGuestProgram(String vmName, String fileDir, String args) {
        return null;
    }

    /**
     * @param vmName          虚拟机名称
     * @param filePathLocal   源位置（本机路径）
     * @param filePathInGuest 目标位置（客户集中的路径）
     * @return 如果成功则返回null，否则返回表示错误信息的字符串
     * @功能描述 将文件上传到虚拟机中
     */
    @WebMethod
    public String UploadFile(String vmName, String filePathLocal, String filePathInGuest) {
        return null;
    }
}
