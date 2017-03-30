package vm.helper;

import com.vmware.vim25.*;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;


/**
 * 
 * 
 * 对虚拟机进行poweron、poweroff、reset、suspend、reboot、shutdown、standby操作
 * 其中operation参数是必须的
 * 而datacentername、guestid、hostname、vmname、all选一个
 * 
 * operation         : 执行的动作  [poweron | poweroff | reset | suspend | reboot | shutdown | standby]
 * datacentername    : 数据中心的名字
 * guestid           : 虚拟机的guesid
 * hostname          : 主机名字
 * vmname            : 虚拟机的名字向指定的虚拟机操作
 * all               : 对所有受控制的虚拟机操作，默认为false [true|false]
 *
 * 
 */

public class VCVMPower extends VCTaskBase {
	private String vmName = null;
	private String operation = null;
	private String datacenter = null;
	private String guestId = null;
	private String host = null;
	private Boolean all = false;

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public void setDatacenter(String datacenter) {
		this.datacenter = datacenter;
	}

	public void setGuestId(String guestId) {
		this.guestId = guestId;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setVmName(String vmName) {
		this.vmName = vmName;
	}

	public void setAll(final Boolean flag) {
		this.all = flag;
	}

	//对是否是参数是否合法进行验证
	private void validate() throws IllegalArgumentException {
		if (all && (vmName != null || datacenter != null || guestId != null || host != null)) {
			logger.error("Did you really mean all? " + "Use '--all true' by itself " +
					"not with --vmname or --datacentername or --guestid or --hostname");
			throw new IllegalArgumentException("--all true occurred in conjunction with other options");
		}
		if (!(operation.equalsIgnoreCase("poweron"))
				&& !(operation.equalsIgnoreCase("poweroff"))
				&& !(operation.equalsIgnoreCase("reset"))
				&& !(operation.equalsIgnoreCase("standby"))
				&& !(operation.equalsIgnoreCase("shutdown"))
				&& !(operation.equalsIgnoreCase("reboot"))
				&& !(operation.equalsIgnoreCase("suspend"))) {

			logger.error("Invalid Operation name ' " + operation
					+ "' valid operations are poweron, standby,"
					+ " poweroff, standby, reboot, shutdown, suspend");
			throw new IllegalArgumentException("Invalid Operation Type Or Name");
		}
	}

	/**
	 * 获得单个vm或者datacenter中所有的vm或者host中所有的vm!
	 **/
	private Map<String, ManagedObjectReference> getVms() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
		Map<String, ManagedObjectReference> vmList = new HashMap<>();

		// Start from the root folder
		ManagedObjectReference container = serviceContent.getRootFolder();
		if (datacenter != null) {
			Map<String, ManagedObjectReference> datacenters =
					VCHelper.inContainerByType(container, "Datacenter");
			logger.info("Number of datacenters found: " + datacenters.size());
			ManagedObjectReference dcMoref = datacenters.get(datacenter);
			if (dcMoref == null) {
				logger.error("No datacenter by the name " + datacenter
						+ " found!");
			}
			container = dcMoref;
		}

		if (host != null) {
			ManagedObjectReference hostMoref =
					VCHelper.inContainerByType(container, "HostSystem").get(host);
			if (hostMoref == null) {
				logger.error("No host by the name " + host + " found!");
				return vmList;
			}
			container = hostMoref;
		}

		Map<String, ManagedObjectReference> vms =
				VCHelper.inContainerByType(container, "VirtualMachine");

		if (vmName != null) {
			if (vms.containsKey(vmName)) {
				vmList.put(vmName, vms.get(vmName));
			} else {
				throw new IllegalStateException("No VM by the name of '" + vmName + "' found!");
			}
			return vmList;
		}

		if (guestId != null) {
			Map<ManagedObjectReference, Map<String, Object>> vmListProp =
					VCHelper.entityProps(
							new ArrayList<>(vms.values()),
							new String[]{"summary.config.guestId", "name"});
			for (ManagedObjectReference vmRef : vmListProp.keySet()) {
				if (guestId.equalsIgnoreCase((String) vmListProp.get(vmRef).get(
						"summary.config.guestId"))) {
					vmList.put((String) vmListProp.get(vmRef).get("name"), vmRef);
				}
			}
			return vmList;
		}

		// If no filters are there then just the container based containment is used.
		vmList = vms;

		return vmList;
	}

	private void runOperation() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
		Map<String, ManagedObjectReference> vmMap = getVms();
		if (vmMap == null || vmMap.isEmpty()) {
			logger.error("No Virtual Machine found matching "
					+ "the specified criteria");
		} else {
			if (operation.equalsIgnoreCase("poweron")) {
				powerOnVM(vmMap);
			} else if (operation.equalsIgnoreCase("poweroff")) {
				powerOffVM(vmMap);
			} else if (operation.equalsIgnoreCase("reset")) {
				resetVM(vmMap);
			} else if (operation.equalsIgnoreCase("suspend")) {
				suspendVM(vmMap);
			} else if (operation.equalsIgnoreCase("reboot")) {
				rebootVM(vmMap);
			} else if (operation.equalsIgnoreCase("shutdown")) {
				shutdownVM(vmMap);
			} else if (operation.equalsIgnoreCase("standby")) {
				standbyVM(vmMap);
			}
		}
	}

	private void powerOnVM(Map<String, ManagedObjectReference> vmMap) {
		for (String vmname : vmMap.keySet()) {
			ManagedObjectReference vmMor = vmMap.get(vmname);
			try {
				logger.info("Powering on virtual machine : " + vmname + "["
						+ vmMor.getValue() + "]");
				ManagedObjectReference taskmor = vimPort.powerOnVMTask(vmMor, null);
				if (getTaskResultAfterDone(taskmor)) {
					logger.info(vmname + "[" + vmMor.getValue()
							+ "] powered on successfully");
				}
			} catch (Exception e) {
				logger.error("Unable to poweron vm : " + vmname + "["
						+ vmMor.getValue() + "]");
				logger.error("Reason :" + e.getLocalizedMessage());
			}
		}
	}

	private void powerOffVM(Map<String, ManagedObjectReference> vmMap) {
		for (String vmname : vmMap.keySet()) {
			ManagedObjectReference vmMor = vmMap.get(vmname);
			try {
				logger.info("Powering off virtual machine : " + vmname + "["
						+ vmMor.getValue() + "]");
				ManagedObjectReference taskmor = vimPort.powerOffVMTask(vmMor);
				if (getTaskResultAfterDone(taskmor)) {
					logger.info(vmname + "[" + vmMor.getValue()
							+ "] powered off successfully");
				}
			} catch (Exception e) {
				logger.error("Unable to poweroff vm : " + vmname + "["
						+ vmMor.getValue() + "]");
				logger.error("Reason :" + e.getLocalizedMessage());
			}
		}
	}

	private void resetVM(Map<String, ManagedObjectReference> vmMap) {
		for (String vmname : vmMap.keySet()) {
			ManagedObjectReference vmMor = vmMap.get(vmname);
			try {
				logger.info("Reseting virtual machine : " + vmname + "["
						+ vmMor.getValue() + "]");
				ManagedObjectReference taskmor = vimPort.resetVMTask(vmMor);
				if (getTaskResultAfterDone(taskmor)) {
					logger.info(vmname + "[" + vmMor.getValue()
							+ "] reset successfully");
				}
			} catch (Exception e) {
				logger.error("Unable to reset vm : " + vmname + "["
						+ vmMor.getValue() + "]");
				System.err.println("Reason :" + e.getLocalizedMessage());
			}
		}
	}

	private void suspendVM(Map<String, ManagedObjectReference> vmMap) {
		for (String vmname : vmMap.keySet()) {
			ManagedObjectReference vmMor = vmMap.get(vmname);
			try {
				logger.info("Suspending virtual machine : " + vmname + "["
						+ vmMor.getValue() + "]");
				ManagedObjectReference taskmor = vimPort.suspendVMTask(vmMor);
				if (getTaskResultAfterDone(taskmor)) {
					logger.info(vmname + "[" + vmMor.getValue()
							+ "] suspended successfully");
				}
			} catch (Exception e) {
				logger.error("Unable to suspend vm : " + vmname + "["
						+ vmMor.getValue() + "]");
				System.err.println("Reason :" + e.getLocalizedMessage());
			}
		}
	}

	private void rebootVM(Map<String, ManagedObjectReference> vmMap) {
		for (String vmname : vmMap.keySet()) {
			ManagedObjectReference vmMor = vmMap.get(vmname);
			try {
				logger.info("Rebooting guest os in virtual machine : "
						+ vmname + "[" + vmMor.getValue() + "]");
				vimPort.rebootGuest(vmMor);
				logger.info("Guest os in vm : " + vmname + "["
						+ vmMor.getValue() + "]" + " rebooted");
			} catch (Exception e) {
				logger.error("Unable to reboot guest os in vm : " + vmname
						+ "[" + vmMor.getValue() + "]");
				System.err.println("Reason :" + e.getLocalizedMessage());
			}
		}
	}

	private void shutdownVM(Map<String, ManagedObjectReference> vmMap) {
		for (String vmname : vmMap.keySet()) {
			ManagedObjectReference vmMor = vmMap.get(vmname);
			try {
				logger.info("Shutting down guest os in virtual machine : "
						+ vmname + "[" + vmMor.getValue() + "]");
				vimPort.shutdownGuest(vmMor);
				logger.info("Guest os in vm : " + vmname + "["
						+ vmMor.getValue() + "]" + " shutdown");
			} catch (Exception e) {
				logger.error("Unable to shutdown guest os in vm : " + vmname
						+ "[" + vmMor.getValue() + "]");
				System.err.println("Reason :" + e.getLocalizedMessage());
			}
		}
	}

	private void standbyVM(Map<String, ManagedObjectReference> vmMap) {
		for (String vmname : vmMap.keySet()) {
			ManagedObjectReference vmMor = vmMap.get(vmname);
			try {
				logger.info("Putting the guest os in virtual machine : "
						+ vmname + "[" + vmMor.getValue() + "] in standby mode");
				vimPort.standbyGuest(vmMor);
				logger.info("Guest os in vm : " + vmname + "["
						+ vmMor.getValue() + "]" + " in standby mode");
			} catch (Exception e) {
				logger.error("Unable to put the guest os in vm : " + vmname
						+ "[" + vmMor.getValue() + "] to standby mode");
				System.err.println("Reason :" + e.getLocalizedMessage());
			}
		}
	}

	public void run() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, InvalidLoginFaultMsg, NoSuchAlgorithmException, InvalidLocaleFaultMsg, KeyManagementException {
		validate();
		if (checkOptions()) {
			init();
			runOperation();
			VCClientSession.Disconnect();
		}
	}

	private boolean checkOptions() {
		boolean run = false;

		if (all) {
			// force operations to broadcast to ALL virtual machines.
			vmName = null;
			datacenter = null;
			host = null;
			logger.error("Power operations will be broadcast to ALL virtual machines.");
			run = true;
		} else if (vmName == null
				&& datacenter == null
				&& guestId == null
				&& host == null
				&& System.console() != null) {
			throw new IllegalStateException("You must specify one of --vmname or --datacentername or --hostname or --all");
		} else {
			run = true;
		}

		return run;
	}
}
