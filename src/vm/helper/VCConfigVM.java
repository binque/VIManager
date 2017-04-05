package vm.helper;

import com.vmware.vim25.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by huxia on 2017/3/19.
 * Complete By Huxiao
 */
public class VCConfigVM extends VCTaskBase {

    public void run(String vmName, String CPU, String memory, String diskName, String diskSize, String diskMode) throws Exception {
        try {
            init();
            ManagedObjectReference virtualmachine = VCHelper.vmByVmname(serviceContent, vimPort, vmName, serviceContent.getPropertyCollector());

            if (virtualmachine != null) {
                reConfig(virtualmachine, vmName, CPU, memory, diskName, diskSize, diskMode);
            } else {
                logger.error(String.format("Virtual Machine named [ %s ] not found.", vmName));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            vcClientSession.Disconnect();
        }
    }

    /**
     * @param virtualMachine 虚拟机MOR
     * @param vmName         虚拟机名称
     * @param CPU            给虚拟机分配的cpu核心数目，可以是一个数字或者low(1)/normal(2)/high(4)
     * @param memory         给虚拟机分配的内存大小，可以是一个数字或者low(1024)/normal(2048)/high(4096)
     * @param diskName       给虚拟机增加或者移除的磁盘名称 虚拟磁盘模式(diskmode)、虚拟磁盘大小(disksize)
     * @param diskSize       磁盘大小，如果指定了磁盘大小，则增加磁盘，如果未指定磁盘大小，则移除磁盘
     * @param diskMode       磁盘模式，可选 persistent/independent_persistent/independent_nonpersistent/nonpersistent/undoable/append
     * @功能描述 重新配置虚拟机
     */
    private void reConfig(ManagedObjectReference virtualMachine, String vmName, String CPU, String memory, String diskName, String diskSize, String diskMode) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg, InvalidStateFaultMsg, InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg, TaskInProgressFaultMsg, DuplicateNameFaultMsg, FileFaultFaultMsg, InvalidNameFaultMsg, ConcurrentAccessFaultMsg, VmConfigFaultFaultMsg {
        VirtualMachineConfigSpec virtualMachineConfigSpec = new VirtualMachineConfigSpec();
        if (memory != null && !memory.isEmpty()) {
            logger.info(String.format("Reconfiguring The Virtual Machine [ %s ] For Memory Update By Value [ %s ]", vmName, memory));
            try {
                virtualMachineConfigSpec.setMemoryAllocation(getShares(memory));
                long memoryMB;
                if (memory.equalsIgnoreCase("high")) {
                    memoryMB = 4098;
                } else if (memory.equalsIgnoreCase("normal")) {
                    memoryMB = 2048;
                } else if (memory.equalsIgnoreCase("low")) {
                    memoryMB = 1024;
                } else {
                    memoryMB = Integer.parseInt(memory);
                }
                virtualMachineConfigSpec.setMemoryMB(memoryMB);
            } catch (NumberFormatException e) {
                logger.error("Value of Memory update must be one of high|low|normal|[numeric value]");
                return;
            }
        }
        if (CPU != null && !CPU.isEmpty()) {
            logger.info(String.format("Reconfiguring The Virtual Machine [ %s ] For CPU Update By Value [ %s ]", vmName, CPU));
            try {
                virtualMachineConfigSpec.setCpuAllocation(getShares(CPU));
                int numCpu;
                if (CPU.equalsIgnoreCase("high")) {
                    numCpu = 4;
                } else if (CPU.equalsIgnoreCase("normal")) {
                    numCpu = 2;
                } else if (CPU.equalsIgnoreCase("low")) {
                    numCpu = 1;
                } else {
                    numCpu = Integer.parseInt(CPU);
                }
                virtualMachineConfigSpec.setNumCPUs(numCpu);
                virtualMachineConfigSpec.setNumCoresPerSocket(numCpu);
            } catch (NumberFormatException e) {
                logger.error("Value of CPU update must be one of high|low|normal|[numeric value]");
                return;
            }
        }
        if (diskName != null && !diskName.isEmpty()) {
            logger.info(String.format("Reconfiguring The Virtual Machine [ %s ] For Disk Update By Name [ %s ]、 Size [ %s ]、 Mode [ %s ]", vmName, diskName, diskSize, diskMode));
            VirtualDeviceConfigSpec virtualDeviceConfigSpec;
            if (diskSize != null && !diskSize.isEmpty()) {
                if (diskMode == null || diskMode.isEmpty()) {
                    diskMode = "persistent";
                }
                virtualDeviceConfigSpec = getDiskDeviceConfigSpec(virtualMachine, vmName, "add", diskName, diskSize, diskMode);
            } else {
                virtualDeviceConfigSpec = getDiskDeviceConfigSpec(virtualMachine, vmName, "remove", diskName, diskSize, diskMode);
            }
            if (virtualDeviceConfigSpec != null) {
                virtualMachineConfigSpec.getDeviceChange().add(virtualDeviceConfigSpec);
            } else {
                logger.error("Cannot Get Virtual Disk Config Spec.");
                return;
            }
        } /*else if (deviceType.equalsIgnoreCase("nic") && !operation.equalsIgnoreCase("update")) {
            logger.info(String.format("Reconfiguring The Virtual Machine [ %s ] For NIC Update", vmName));
            VirtualDeviceConfigSpec virtualDeviceConfigSpec = getNICDeviceConfigSpec(virtualMachine, operation, value);
            if (virtualDeviceConfigSpec != null) {
                virtualMachineConfigSpec.getDeviceChange().add(virtualDeviceConfigSpec);
            } else {
                return;
            }
        } else if (deviceType.equalsIgnoreCase("cd") && !operation.equalsIgnoreCase("update")) {
            logger.info(String.format("Reconfiguring The Virtual Machine [ %s ] For CD Update", vmName));
            VirtualDeviceConfigSpec virtualDeviceConfigSpec = getCDDeviceConfigSpec(virtualMachine, operation, value);
            if (virtualDeviceConfigSpec != null) {
                virtualMachineConfigSpec.getDeviceChange().add(virtualDeviceConfigSpec);
            } else {
                return;
            }
        } else {
            logger.error("Invalid device type [ memory | cpu | disk | nic | cd ] is :" + deviceType);
            return;
        }
*/
        ManagedObjectReference tmor = vimPort.reconfigVMTask(virtualMachine, virtualMachineConfigSpec);
        if (getTaskResultAfterDone(tmor)) {
            logger.info("Virtual Machine reconfigured successfully");
        } else {
            logger.error("Virtual Machine reconfigur failed");
        }
    }

    private ResourceAllocationInfo getShares(String value) {
        ResourceAllocationInfo raInfo = new ResourceAllocationInfo();
        SharesInfo sharesInfo = new SharesInfo();
        if (value.equalsIgnoreCase(SharesLevel.HIGH.toString())) {
            sharesInfo.setLevel(SharesLevel.HIGH);
        } else if (value.equalsIgnoreCase(SharesLevel.NORMAL.toString())) {
            sharesInfo.setLevel(SharesLevel.NORMAL);
        } else if (value.equalsIgnoreCase(SharesLevel.LOW.toString())) {
            sharesInfo.setLevel(SharesLevel.LOW);
        } else {
            sharesInfo.setLevel(SharesLevel.CUSTOM);
            sharesInfo.setShares(Integer.parseInt(value));
        }
        raInfo.setShares(sharesInfo);
        return raInfo;
    }

    /**
     * @param virtualMachine 虚拟机的MOR
     * @param vmName         虚拟机名称
     * @param operation      操作名称，对于磁盘可以是 add/remove 两者之一
     * @param value          磁盘名称，要增加的或者要移除的磁盘名称，无需后缀名
     * @param disksize       磁盘大小，单位为MB
     * @param diskmode       磁盘模式
     * @return 返回磁盘设备配置SPEC
     */
    private VirtualDeviceConfigSpec getDiskDeviceConfigSpec(ManagedObjectReference virtualMachine, String vmName, String operation, String value, String disksize, String diskmode)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();

        if (operation.equalsIgnoreCase("Add")) {
            VirtualDisk disk = new VirtualDisk();
            VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();
            DatastoreSummary dsSummary = getDatastoreNameWithFreeSpace(virtualMachine, Integer.parseInt(disksize));
            String dsName = dsSummary.getName();

            int ckey = 0;
            int unitNumber = 0;
            List<Integer> getControllerKeyReturnArr = getControllerKey(virtualMachine);
            if (!getControllerKeyReturnArr.isEmpty()) {
                ckey = getControllerKeyReturnArr.get(0);
                unitNumber = getControllerKeyReturnArr.get(1);
            }
            String fileName = "[" + dsName + "] " + vmName + "/" + value + ".vmdk";
            diskfileBacking.setFileName(fileName);
            diskfileBacking.setDiskMode(diskmode);

            disk.setControllerKey(ckey);
            disk.setUnitNumber(unitNumber);
            disk.setBacking(diskfileBacking);
            int size = 1024 * (Integer.parseInt(disksize));
            disk.setCapacityInKB(size);
            disk.setKey(-1);

            diskSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
            diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);
            diskSpec.setDevice(disk);
        } else if (operation.equalsIgnoreCase("Remove")) {
            VirtualDisk disk = null;
            List<VirtualDevice> deviceList = ((ArrayOfVirtualDevice) VCHelper.entityProps(serviceContent, vimPort, virtualMachine, new String[]{"config.hardware.device"}).get("config.hardware.device")).getVirtualDevice();
            for (VirtualDevice device : deviceList) {
                if (device instanceof VirtualDisk) {
                    if (value.equalsIgnoreCase(device.getDeviceInfo().getLabel())) {
                        disk = (VirtualDisk) device;
                        break;
                    }
                }
            }
            if (disk != null) {
                diskSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
                diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.DESTROY);
                diskSpec.setDevice(disk);
            } else {
                logger.error("No device found " + value);
                return null;
            }
        }
        return diskSpec;
    }

    /**
     * @param vmMor 虚拟机MOR
     * @return 返回密匙
     * @功能描述 获取控制器密钥和SCSI控制器上的下一个可用空闲单元号
     */
    private List<Integer> getControllerKey(ManagedObjectReference vmMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        List<Integer> retVal = new ArrayList<>();

        List<VirtualDevice> listvd = ((ArrayOfVirtualDevice) VCHelper.entityProps(serviceContent, vimPort, vmMor, new String[]{"config.hardware.device"}).get("config.hardware.device")).getVirtualDevice();

        Map<Integer, VirtualDevice> deviceMap = new HashMap<>();
        for (VirtualDevice virtualDevice : listvd) {
            deviceMap.put(virtualDevice.getKey(), virtualDevice);
        }
        boolean found = false;
        for (VirtualDevice virtualDevice : listvd) {
            if (virtualDevice instanceof VirtualSCSIController) {
                VirtualSCSIController vscsic = (VirtualSCSIController) virtualDevice;
                int[] slots = new int[16];
                slots[7] = 1;
                List<Integer> devicelist = vscsic.getDevice();
                for (Integer deviceKey : devicelist) {
                    if (deviceMap.get(deviceKey).getUnitNumber() != null) {
                        slots[deviceMap.get(deviceKey).getUnitNumber()] = 1;
                    }
                }
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] != 1) {
                        retVal.add(vscsic.getKey());
                        retVal.add(i);
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }

        if (!found) {
            throw new RuntimeException(
                    "The SCSI controller on the vm has maxed out its "
                            + "capacity. Please add an additional SCSI controller");
        }
        return retVal;
    }

    private VirtualDeviceConfigSpec getCDDeviceConfigSpec(ManagedObjectReference virtualMachine, String operation, String value)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        VirtualDeviceConfigSpec cdSpec = new VirtualDeviceConfigSpec();
        List<VirtualDevice> listvd = ((ArrayOfVirtualDevice) VCHelper.entityProps(serviceContent, vimPort, virtualMachine, new String[]{"config.hardware.device"}).get("config.hardware.device")).getVirtualDevice();

        if (operation.equalsIgnoreCase("Add")) {
            cdSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            VirtualCdrom cdrom = new VirtualCdrom();

            VirtualCdromRemoteAtapiBackingInfo vcrabi = new VirtualCdromRemoteAtapiBackingInfo();
            vcrabi.setDeviceName("");
            vcrabi.setUseAutoDetect(true);

            Map<Integer, VirtualDevice> deviceMap = new HashMap<>();
            for (VirtualDevice virtualDevice : listvd) {
                deviceMap.put(virtualDevice.getKey(), virtualDevice);
            }
            int controllerKey = 0;
            int unitNumber = 0;
            boolean found = false;
            for (VirtualDevice virtualDevice : listvd) {
                if (virtualDevice instanceof VirtualIDEController) {
                    VirtualIDEController vscsic = (VirtualIDEController) virtualDevice;
                    int[] slots = new int[2];
                    List<Integer> devicelist = vscsic.getDevice();
                    for (Integer deviceKey : devicelist) {
                        if (deviceMap.get(deviceKey).getUnitNumber() != null) {
                            slots[deviceMap.get(deviceKey).getUnitNumber()] = 1;
                        }
                    }
                    for (int i = 0; i < slots.length; i++) {
                        if (slots[i] != 1) {
                            controllerKey = vscsic.getKey();
                            unitNumber = i;
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
            }

            if (!found) {
                throw new RuntimeException("The IDE controller on the vm has maxed out its capacity. Please add an additional IDE controller");
            }

            cdrom.setBacking(vcrabi);
            cdrom.setControllerKey(controllerKey);
            cdrom.setUnitNumber(unitNumber);
            cdrom.setKey(-1);

            cdSpec.setDevice(cdrom);
            return cdSpec;
        } else {
            VirtualCdrom cdRemove = null;
            cdSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
            for (VirtualDevice device : listvd) {
                if (device instanceof VirtualCdrom) {
                    if (value.equalsIgnoreCase(device.getDeviceInfo()
                            .getLabel())) {
                        cdRemove = (VirtualCdrom) device;
                        break;
                    }
                }
            }
            if (cdRemove != null) {
                cdSpec.setDevice(cdRemove);
            } else {
                logger.error("No device available " + value);
                return null;
            }
        }
        return cdSpec;
    }

    private VirtualDeviceConfigSpec getNICDeviceConfigSpec(ManagedObjectReference virtualMachine, String operation, String value)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
        if (operation.equalsIgnoreCase("Add")) {
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
            VirtualEthernetCard nic = new VirtualPCNet32();
            VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
            nicBacking.setDeviceName(value);
            nic.setAddressType("generated");
            nic.setBacking(nicBacking);
            nic.setKey(-1);
            nicSpec.setDevice(nic);
        } else if (operation.equalsIgnoreCase("Remove")) {
            VirtualEthernetCard nic = null;
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
            List<VirtualDevice> listvd = ((ArrayOfVirtualDevice) VCHelper.entityProps(serviceContent, vimPort, virtualMachine, new String[]{"config.hardware.device"}).get("config.hardware.device")).getVirtualDevice();
            for (VirtualDevice device : listvd) {
                if (device instanceof VirtualEthernetCard) {
                    if (value.equalsIgnoreCase(device.getDeviceInfo().getLabel())) {
                        nic = (VirtualEthernetCard) device;
                        break;
                    }
                }
            }
            if (nic != null) {
                nicSpec.setDevice(nic);
            } else {
                logger.error("No device available " + value);
                return null;
            }
        }
        return nicSpec;
    }

    /**
     * @param operation 操作名称，可以是 add/remove/update 三者之一
     * @param device    所要操作的设备名称，可以使 disk/nic/cd/cpu/memory 五者之一
     * @param value     所要修改的目标值，根据具体情况确定
     * @param disksize  磁盘大小
     * @param diskmode  磁盘模式
     * @return 如果参数有效，则返回true，否则返回false
     * @功能描述 检查参数的有效性，确定业务执行逻辑
     */
    private boolean customValidation(final String operation, final String device,
                                            final String value, final String disksize, final String diskmode) {
        boolean flag = true;
        if (device.equalsIgnoreCase("disk")) {
            if (operation.equalsIgnoreCase("add")) {
                if ((disksize == null) || (diskmode == null)) {
                    logger.error("For add disk operation, disksize and diskmode are the Mandatory options");
                    flag = false;
                }
                if (disksize != null && Integer.parseInt(disksize) <= 0) {
                    logger.error("Disksize must be a greater than zero");
                    flag = false;
                }
            }
            if (operation.equalsIgnoreCase("remove")) {
                if (value == null) {
                    logger.error("Please specify a label in value field to remove the disk");
                }
            }
        }
        if (device.equalsIgnoreCase("nic")) {
            if (operation == null) {
                logger.error("For add nic operation is the Mandatory option");
                flag = false;
            }
        }
        if (device.equalsIgnoreCase("cd")) {
            if (operation == null) {
                logger.error("For add cd operation is the Mandatory options");
                flag = false;
            }
        }
        if (operation != null) {
            if (operation.equalsIgnoreCase("add")
                    || operation.equalsIgnoreCase("remove")
                    || operation.equalsIgnoreCase("update")) {
                if (device.equals("cpu") || device.equals("memory")) {
                    if (!operation.equals("update")) {
                        logger.error("Invalid operation specified for device cpu or memory");
                        flag = false;
                    }
                }
            } else {
                logger.error("Operation must be either add, remove or update");
                flag = false;
            }
        }
        return flag;
    }
}
