package vm.helper;

import com.vmware.vim25.*;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by huxia on 2017/3/19.
 */
public class VCConfigVM extends VCTaskBase {

    public static void run(final String vmName, final String operation, final String device,
                           final String value, final String diskSize, final String diskmode) throws InvalidLoginFaultMsg, NoSuchAlgorithmException, RuntimeFaultFaultMsg, InvalidLocaleFaultMsg, KeyManagementException, InvalidPropertyFaultMsg, InsufficientResourcesFaultFaultMsg, DuplicateNameFaultMsg, TaskInProgressFaultMsg, InvalidStateFaultMsg, ConcurrentAccessFaultMsg, FileFaultFaultMsg, InvalidCollectorVersionFaultMsg, InvalidDatastoreFaultMsg, VmConfigFaultFaultMsg, InvalidNameFaultMsg {
        // 检查参数类型
        if (customValidation(vmName, operation, device, value, diskSize, diskmode)) {
            if (!VCClientSession.IsConnected()) {
                VCClientSession.Connect();
            }

            init();

            ManagedObjectReference virtualmachien = VCHelper.vmByVmname(vmName, serviceContent.getPropertyCollector());

            if (virtualmachien != null) {
                reConfig(virtualmachien, vmName, operation, device, value, diskSize, diskmode);
            } else {
                System.out.printf("Virtual Machine named [ %s ] not found.", vmName);
            }
        }
    }

    /**
     * @功能描述 重新配置虚拟机
     * @param vmName 虚拟机名称
     * @param operation 操作名称，可以是 add/remove/update 三者之一
     * @param deviceType 所要操作的设备名称，可以使 disk/nic/cd/cpu/memory 五者之一
     * @param value 所要修改的目标值，根据具体情况确定
     * @param diskSize 磁盘大小
     * @param diskmode 磁盘模式
     */
    private static void reConfig(ManagedObjectReference virtualMachine, String vmName, String operation, String deviceType, String value, String diskSize, String diskmode) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidCollectorVersionFaultMsg, InvalidStateFaultMsg, InsufficientResourcesFaultFaultMsg, InvalidDatastoreFaultMsg, TaskInProgressFaultMsg, DuplicateNameFaultMsg, FileFaultFaultMsg, InvalidNameFaultMsg, ConcurrentAccessFaultMsg, VmConfigFaultFaultMsg {
        VirtualMachineConfigSpec virtualMachineConfigSpec = new VirtualMachineConfigSpec();

        if (deviceType.equalsIgnoreCase("memory") && operation.equalsIgnoreCase("update")) {
            System.out.printf("Reconfiguring The Virtual Machine [ %s ] For Memory Update", vmName);
            try {
                virtualMachineConfigSpec.setMemoryAllocation(getShares(value));
            } catch (NumberFormatException e) {
                System.out.printf("Value of Memory update must be one of high|low|normal|[numeric value]");
                return;
            }
        } else if (deviceType.equalsIgnoreCase("cpu") && !operation.equalsIgnoreCase("update")) {
            System.out.printf("Reconfiguring The Virtual Machine [ %s ] For CPU Update", vmName);
            try {
                virtualMachineConfigSpec.setCpuAllocation(getShares(value));
            } catch (NumberFormatException e) {
                System.out.printf("Value of CPU update must be one of high|low|normal|[numeric value]");
                return;
            }
        } else if (deviceType.equalsIgnoreCase("disk") && !operation.equalsIgnoreCase("update")) {
            System.out.printf("Reconfiguring The Virtual Machine [ %s ] For Disk Update", vmName);
            VirtualDeviceConfigSpec virtualDeviceConfigSpec = getDiskDeviceConfigSpec(virtualMachine, vmName, operation, value, diskSize, diskmode);
            if (virtualDeviceConfigSpec != null) {
                virtualMachineConfigSpec.getDeviceChange().add(virtualDeviceConfigSpec);
            } else {
                return;
            }
        } else if (deviceType.equalsIgnoreCase("nic") && !operation.equalsIgnoreCase("update")) {
            System.out.printf("Reconfiguring The Virtual Machine [ %s ] For NIC Update", vmName);
            VirtualDeviceConfigSpec virtualDeviceConfigSpec = getNICDeviceConfigSpec(virtualMachine, operation, value);
            if (virtualDeviceConfigSpec != null) {
                virtualMachineConfigSpec.getDeviceChange().add(virtualDeviceConfigSpec);
            } else {
                return;
            }
        } else if (deviceType.equalsIgnoreCase("cd") && !operation.equalsIgnoreCase("update")) {
            System.out.printf("Reconfiguring The Virtual Machine [ %s ] For CD Update", vmName);
            VirtualDeviceConfigSpec virtualDeviceConfigSpec = getCDDeviceConfigSpec(virtualMachine, operation, value);
            if (virtualDeviceConfigSpec != null) {
                virtualMachineConfigSpec.getDeviceChange().add(virtualDeviceConfigSpec);
            } else {
                return;
            }
        } else {
            System.out.println("Invalid device type [ memory | cpu | disk | nic | cd ]");
            return;
        }

        ManagedObjectReference tmor = vimPort.reconfigVMTask(virtualMachine, virtualMachineConfigSpec);
        if (getTaskResultAfterDone(tmor)) {
            System.out.println("Virtual Machine reconfigured successfully");
        } else {
            System.out.println("Virtual Machine reconfigur failed");
        }
    }

    /**
     * @功能描述 获取控制器密钥和SCSI控制器上的下一个可用空闲单元号
     * @param vmMor 虚拟机MOR
     * @return 返回密匙
     */
    private static List<Integer> getControllerKey(ManagedObjectReference vmMor)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        List<Integer> retVal = new ArrayList<>();

        List<VirtualDevice> listvd = ((ArrayOfVirtualDevice) VCHelper.entityProps(vmMor, new String[]{"config.hardware.device"}).get("config.hardware.device")).getVirtualDevice();

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

    private static ResourceAllocationInfo getShares(String value) {
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

    private static String getDatastoreNameWithFreeSpace(ManagedObjectReference virtualMachine, int minFreeSpace)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        String dsName = null;
        List<ManagedObjectReference> datastores = ((ArrayOfManagedObjectReference) VCHelper.entityProps(virtualMachine, new String[] { "datastore" }).get("datastore")).getManagedObjectReference();
        for (ManagedObjectReference datastore : datastores) {
            DatastoreSummary ds = (DatastoreSummary) VCHelper.entityProps(datastore, new String[] { "summary" }).get("summary");
            if (ds.getFreeSpace() > minFreeSpace) {
                dsName = ds.getName();
                break;
            }
        }
        return dsName;
    }

    private static VirtualDeviceConfigSpec getDiskDeviceConfigSpec(ManagedObjectReference virtualMachine, String vmName, String operation, String value, String disksize, String diskmode)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        String ops = operation;
        VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();

        if (ops.equalsIgnoreCase("Add")) {
            VirtualDisk disk = new VirtualDisk();
            VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();
            String dsName = getDatastoreNameWithFreeSpace(virtualMachine, Integer.parseInt(disksize));

            int ckey = 0;
            int unitNumber = 0;
            List<Integer> getControllerKeyReturnArr = getControllerKey(virtualMachine);
            if (!getControllerKeyReturnArr.isEmpty()) {
                ckey = getControllerKeyReturnArr.get(0);
                unitNumber = getControllerKeyReturnArr.get(1);
            }
            String fileName = "[" + dsName + "] " + vmName + "/" + value
                    + ".vmdk";
            diskfileBacking.setFileName(fileName);
            diskfileBacking.setDiskMode(diskmode);

            disk.setControllerKey(ckey);
            disk.setUnitNumber(unitNumber);
            disk.setBacking(diskfileBacking);
            int size = 1024 * (Integer.parseInt(disksize));
            disk.setCapacityInKB(size);
            disk.setKey(-1);

            diskSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
            diskSpec
                    .setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);
            diskSpec.setDevice(disk);
        } else if (ops.equalsIgnoreCase("Remove")) {
            VirtualDisk disk = null;
            List<VirtualDevice> deviceList = ((ArrayOfVirtualDevice) VCHelper.entityProps(virtualMachine, new String[]{"config.hardware.device"}).get("config.hardware.device")).getVirtualDevice();
            for (VirtualDevice device : deviceList) {
                if (device instanceof VirtualDisk) {
                    if (value.equalsIgnoreCase(device.getDeviceInfo()
                            .getLabel())) {
                        disk = (VirtualDisk) device;
                        break;
                    }
                }
            }
            if (disk != null) {
                diskSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
                diskSpec
                        .setFileOperation(VirtualDeviceConfigSpecFileOperation.DESTROY);
                diskSpec.setDevice(disk);
            } else {
                System.out.println("No device found " + value);
                return null;
            }
        }
        return diskSpec;
    }

    private static VirtualDeviceConfigSpec getCDDeviceConfigSpec(ManagedObjectReference virtualMachine, String operation, String value)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        String ops = operation;
        VirtualDeviceConfigSpec cdSpec = new VirtualDeviceConfigSpec();
        List<VirtualDevice> listvd = ((ArrayOfVirtualDevice) VCHelper.entityProps(virtualMachine, new String[]{"config.hardware.device"}).get("config.hardware.device")).getVirtualDevice();

        if (ops.equalsIgnoreCase("Add")) {
            cdSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            VirtualCdrom cdrom = new VirtualCdrom();

            VirtualCdromRemoteAtapiBackingInfo vcrabi = new VirtualCdromRemoteAtapiBackingInfo();
            vcrabi.setDeviceName("");
            vcrabi.setUseAutoDetect(true);

            Map<Integer, VirtualDevice> deviceMap = new HashMap<Integer, VirtualDevice>();
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
                System.out.println("No device available " + value);
                return null;
            }
        }
        return cdSpec;
    }

    private static VirtualDeviceConfigSpec getNICDeviceConfigSpec(ManagedObjectReference virtualMachine, String operation, String value)
            throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        String ops = operation;
        VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
        if (ops.equalsIgnoreCase("Add")) {
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
            VirtualEthernetCard nic = new VirtualPCNet32();
            VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
            nicBacking.setDeviceName(value);
            nic.setAddressType("generated");
            nic.setBacking(nicBacking);
            nic.setKey(-1);
            nicSpec.setDevice(nic);
        } else if (ops.equalsIgnoreCase("Remove")) {
            VirtualEthernetCard nic = null;
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
            List<VirtualDevice> listvd = ((ArrayOfVirtualDevice) VCHelper.entityProps(virtualMachine, new String[]{"config.hardware.device"}).get("config.hardware.device")).getVirtualDevice();
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
                System.out.println("No device available " + value);
                return null;
            }
        }
        return nicSpec;
    }

    /**
     * @功能说明 检查参数的有效性，确定业务执行逻辑
     * @param vmName 虚拟机名称
     * @param operation 操作名称，可以是 add/remove/update 三者之一
     * @param device 所要操作的设备名称，可以使 disk/nic/cd/cpu/memory 五者之一
     * @param value 所要修改的目标值，根据具体情况确定
     * @param disksize 磁盘大小
     * @param diskmode 磁盘模式
     * @return 如果参数有效，则返回true，否则返回false
     */
    private static boolean customValidation(final String vmName, final String operation, final String device,
                                            final String value, final String disksize, final String diskmode) {
        boolean flag = true;
        if (device.equalsIgnoreCase("disk")) {
            if (operation.equalsIgnoreCase("add")) {
                if ((disksize == null) || (diskmode == null)) {
                    System.out.println("For add disk operation, disksize and diskmode are the Mandatory options");
                    flag = false;
                }
                if (disksize != null && Integer.parseInt(disksize) <= 0) {
                    System.out.println("Disksize must be a greater than zero");
                    flag = false;
                }
            }
            if (operation.equalsIgnoreCase("remove")) {
                if (value == null) {
                    System.out.println("Please specify a label in value field to remove the disk");
                }
            }
        }
        if (device.equalsIgnoreCase("nic")) {
            if (operation == null) {
                System.out.println("For add nic operation is the Mandatory option");
                flag = false;
            }
        }
        if (device.equalsIgnoreCase("cd")) {
            if (operation == null) {
                System.out.println("For add cd operation is the Mandatory options");
                flag = false;
            }
        }
        if (operation != null) {
            if (operation.equalsIgnoreCase("add")
                    || operation.equalsIgnoreCase("remove")
                    || operation.equalsIgnoreCase("update")) {
                if (device.equals("cpu") || device.equals("memory")) {
                    if (!operation.equals("update")) {
                        System.out.println("Invalid operation specified for device cpu or memory");
                        flag = false;
                    }
                }
            } else {
                System.out.println("Operation must be either add, remove or update");
                flag = false;
            }
        }
        return flag;
    }
}
