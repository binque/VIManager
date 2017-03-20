package vm.helper;

import java.util.*;

import com.vmware.vim25.*;


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

public class VCVMPower {
	String vmName = null;
	String operation = null;
	String datacenter = null;
	String guestId = null;
	String host = null;
	Boolean all = false;
	
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
	 void validate() throws IllegalArgumentException {
	        if( all && (vmName != null || datacenter != null || guestId != null || host != null) ) {
	            System.out.println("Did you really mean all? " + "Use '--all true' by itself " +
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

	            System.out.println("Invalid Operation name ' " + operation
	                    + "' valid operations are poweron, standby,"
	                    + " poweroff, standby, reboot, shutdown, suspend");
	            throw new IllegalArgumentException("Invalid Operation Type Or Name");
	        }
	 }
	 
	 /**
	     * 获取所有的  MOREFs 对象
	     *
	     * @param folder    开始的目录
	     * @param morefType 类型
	     * @return Map<String, ManagedObjectReference>由名字和MOR对象组成不存在则返回empty Map
	     * 
	     * @throws InvalidPropertyFaultMsg
	     * @throws RuntimeFaultFaultMsg
	     */
	 Map<String, ManagedObjectReference> getMOREFsInContainerByType(
	            ManagedObjectReference folder, String morefType)
	            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
	        String PROP_ME_NAME = "name";
	        ManagedObjectReference viewManager = VCClientSession.getServiceContent().getViewManager();
	        ManagedObjectReference containerView =
	                VCClientSession.getVimPort().createContainerView(viewManager, folder,
	                        Arrays.asList(morefType), true);

	        Map<String, ManagedObjectReference> tgtMoref =
	                new HashMap<String, ManagedObjectReference>();

	        // Create Property Spec
	        PropertySpec propertySpec = new PropertySpec();
	        propertySpec.setAll(Boolean.FALSE);
	        propertySpec.setType(morefType);
	        propertySpec.getPathSet().add(PROP_ME_NAME);

	        TraversalSpec ts = new TraversalSpec();
	        ts.setName("view");
	        ts.setPath("view");
	        ts.setSkip(false);
	        ts.setType("ContainerView");

	        // Now create Object Spec
	        ObjectSpec objectSpec = new ObjectSpec();
	        objectSpec.setObj(containerView);
	        objectSpec.setSkip(Boolean.TRUE);
	        objectSpec.getSelectSet().add(ts);

	        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
	        // created above.
	        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
	        propertyFilterSpec.getPropSet().add(propertySpec);
	        propertyFilterSpec.getObjectSet().add(objectSpec);

	        List<PropertyFilterSpec> propertyFilterSpecs =
	                new ArrayList<PropertyFilterSpec>();
	        propertyFilterSpecs.add(propertyFilterSpec);

	        RetrieveResult rslts =
	               VCClientSession.getVimPort().retrievePropertiesEx(VCClientSession.getServiceContent().getPropertyCollector(),
	                        propertyFilterSpecs, new RetrieveOptions());
	        List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();
	        if (rslts != null && rslts.getObjects() != null
	                && !rslts.getObjects().isEmpty()) {
	            listobjcontent.addAll(rslts.getObjects());
	        }
	        String token = null;
	        if (rslts != null && rslts.getToken() != null) {
	            token = rslts.getToken();
	        }
	        while (token != null && !token.isEmpty()) {
	            rslts =
	            		VCClientSession.getVimPort().continueRetrievePropertiesEx(
	            				VCClientSession.getServiceContent().getPropertyCollector(), token);
	            token = null;
	            if (rslts != null) {
	                token = rslts.getToken();
	                if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
	                    listobjcontent.addAll(rslts.getObjects());
	                }
	            }
	        }
	        for (ObjectContent oc : listobjcontent) {
	            ManagedObjectReference mr = oc.getObj();
	            String entityNm = null;
	            List<DynamicProperty> dps = oc.getPropSet();
	            if (dps != null) {
	                for (DynamicProperty dp : dps) {
	                    entityNm = (String) dp.getVal();
	                }
	            }
	            tgtMoref.put(entityNm, mr);
	        }
	        return tgtMoref;
	 }
	 
	 //执行结果成功与否的说明
	 boolean getTaskResultAfterDone(ManagedObjectReference task)
	            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg,
	            InvalidCollectorVersionFaultMsg {

	        boolean retVal = false;

	        // info has a property - state for state of the task
	        Object[] result =
	                VCClientSession.WaitForValues(task, new String[]{"info.state", "info.error"},
	                        new String[]{"state"}, new Object[][]{new Object[]{
	                        TaskInfoState.SUCCESS, TaskInfoState.ERROR}});

	        if (result[0].equals(TaskInfoState.SUCCESS)) {
	            retVal = true;
	        }
	        if (result[1] instanceof LocalizedMethodFault) {
	            throw new RuntimeException(
	                    ((LocalizedMethodFault) result[1]).getLocalizedMessage());
	        }
	        return retVal;
	 }
	 
	 
	 /**
	     * 获得单个vm或者datacenter中所有的vm或者host中所有的vm!
	 **/
	 Map<String, ManagedObjectReference> getVms() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
	        Map<String, ManagedObjectReference> vmList =
	                new HashMap<String, ManagedObjectReference>();

	        // Start from the root folder
	        ManagedObjectReference container = VCClientSession.getServiceContent().getRootFolder();
	        if (datacenter != null) {
	            Map<String,ManagedObjectReference> datacenters =
	            		VCHelper.inContainerByType(container, "Datacenter");
	            System.out.println("Number of datacenters found: " + datacenters.size());
	            ManagedObjectReference dcMoref = datacenters.get(datacenter);
	            if (dcMoref == null) {
	                System.out.println("No datacenter by the name " + datacenter
	                        + " found!");
	            }
	            container = dcMoref;
	        }

	        if (host != null) {
	            ManagedObjectReference hostMoref =
	                    VCHelper.inContainerByType(container, "HostSystem").get(host);
	            if (hostMoref == null) {
	                System.out.println("No host by the name " + host + " found!");
	                return vmList;
	            }
	            container = hostMoref;
	        }

	        Map<String, ManagedObjectReference> vms =
	                VCHelper.inContainerByType(container,"VirtualMachine");

	        if (vmName != null) {
	            if (vms.containsKey(vmName)) {
	                vmList.put(vmName, vms.get(vmName));
	            }
	            else {
	                throw new IllegalStateException("No VM by the name of '" + vmName + "' found!");
	            }
	            return vmList;
	        }

	        if (guestId != null) {
	            Map<ManagedObjectReference, Map<String, Object>> vmListProp =
	                   VCHelper.entityProps(
	                            new ArrayList<ManagedObjectReference>(vms.values()),
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
	 
	 void runOperation() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
	        Map<String, ManagedObjectReference> vmMap = getVms();
	        if (vmMap == null || vmMap.isEmpty()) {
	            System.out.println("No Virtual Machine found matching "
	                    + "the specified criteria");
	            return;
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

	    void powerOnVM(Map<String, ManagedObjectReference> vmMap) {
	        for (String vmname : vmMap.keySet()) {
	            ManagedObjectReference vmMor = vmMap.get(vmname);
	            try {
	                System.out.println("Powering on virtual machine : " + vmname + "["
	                        + vmMor.getValue() + "]");
	                ManagedObjectReference taskmor = VCClientSession.getVimPort().powerOnVMTask(vmMor, null);
	                if (getTaskResultAfterDone(taskmor)) {
	                    System.out.println(vmname + "[" + vmMor.getValue()
	                            + "] powered on successfully");
	                }
	            } catch (Exception e) {
	                System.out.println("Unable to poweron vm : " + vmname + "["
	                        + vmMor.getValue() + "]");
	                System.err.println("Reason :" + e.getLocalizedMessage());
	            }
	        }
	    }

	    void powerOffVM(Map<String, ManagedObjectReference> vmMap) {
	        for (String vmname : vmMap.keySet()) {
	            ManagedObjectReference vmMor = vmMap.get(vmname);
	            try {
	                System.out.println("Powering off virtual machine : " + vmname + "["
	                        + vmMor.getValue() + "]");
	                ManagedObjectReference taskmor = VCClientSession.getVimPort().powerOffVMTask(vmMor);
	                if (getTaskResultAfterDone(taskmor)) {
	                    System.out.println(vmname + "[" + vmMor.getValue()
	                            + "] powered off successfully");
	                }
	            } catch (Exception e) {
	                System.out.println("Unable to poweroff vm : " + vmname + "["
	                        + vmMor.getValue() + "]");
	                System.err.println("Reason :" + e.getLocalizedMessage());
	            }
	        }
	    }

	    void resetVM(Map<String, ManagedObjectReference> vmMap) {
	        for (String vmname : vmMap.keySet()) {
	            ManagedObjectReference vmMor = vmMap.get(vmname);
	            try {
	                System.out.println("Reseting virtual machine : " + vmname + "["
	                        + vmMor.getValue() + "]");
	                ManagedObjectReference taskmor = VCClientSession.getVimPort().resetVMTask(vmMor);
	                if (getTaskResultAfterDone(taskmor)) {
	                    System.out.println(vmname + "[" + vmMor.getValue()
	                            + "] reset successfully");
	                }
	            } catch (Exception e) {
	                System.out.println("Unable to reset vm : " + vmname + "["
	                        + vmMor.getValue() + "]");
	                System.err.println("Reason :" + e.getLocalizedMessage());
	            }
	        }
	    }

	    void suspendVM(Map<String, ManagedObjectReference> vmMap) {
	        for (String vmname : vmMap.keySet()) {
	            ManagedObjectReference vmMor = vmMap.get(vmname);
	            try {
	                System.out.println("Suspending virtual machine : " + vmname + "["
	                        + vmMor.getValue() + "]");
	                ManagedObjectReference taskmor = VCClientSession.getVimPort().suspendVMTask(vmMor);
	                if (getTaskResultAfterDone(taskmor)) {
	                    System.out.println(vmname + "[" + vmMor.getValue()
	                            + "] suspended successfully");
	                }
	            } catch (Exception e) {
	                System.out.println("Unable to suspend vm : " + vmname + "["
	                        + vmMor.getValue() + "]");
	                System.err.println("Reason :" + e.getLocalizedMessage());
	            }
	        }
	    }

	    void rebootVM(Map<String, ManagedObjectReference> vmMap) {
	        for (String vmname : vmMap.keySet()) {
	            ManagedObjectReference vmMor = vmMap.get(vmname);
	            try {
	                System.out.println("Rebooting guest os in virtual machine : "
	                        + vmname + "[" + vmMor.getValue() + "]");
	                VCClientSession.getVimPort().rebootGuest(vmMor);
	                System.out.println("Guest os in vm : " + vmname + "["
	                        + vmMor.getValue() + "]" + " rebooted");
	            } catch (Exception e) {
	                System.out.println("Unable to reboot guest os in vm : " + vmname
	                        + "[" + vmMor.getValue() + "]");
	                System.err.println("Reason :" + e.getLocalizedMessage());
	            }
	        }
	    }

	    void shutdownVM(Map<String, ManagedObjectReference> vmMap) {
	        for (String vmname : vmMap.keySet()) {
	            ManagedObjectReference vmMor = vmMap.get(vmname);
	            try {
	                System.out.println("Shutting down guest os in virtual machine : "
	                        + vmname + "[" + vmMor.getValue() + "]");
	                VCClientSession.getVimPort().shutdownGuest(vmMor);
	                System.out.println("Guest os in vm : " + vmname + "["
	                        + vmMor.getValue() + "]" + " shutdown");
	            } catch (Exception e) {
	                System.out.println("Unable to shutdown guest os in vm : " + vmname
	                        + "[" + vmMor.getValue() + "]");
	                System.err.println("Reason :" + e.getLocalizedMessage());
	            }
	        }
	    }

	    void standbyVM(Map<String, ManagedObjectReference> vmMap) {
	        for (String vmname : vmMap.keySet()) {
	            ManagedObjectReference vmMor = vmMap.get(vmname);
	            try {
	                System.out.println("Putting the guest os in virtual machine : "
	                        + vmname + "[" + vmMor.getValue() + "] in standby mode");
	                VCClientSession.getVimPort().standbyGuest(vmMor);
	                System.out.println("Guest os in vm : " + vmname + "["
	                        + vmMor.getValue() + "]" + " in standby mode");
	            } catch (Exception e) {
	                System.out.println("Unable to put the guest os in vm : " + vmname
	                        + "[" + vmMor.getValue() + "] to standby mode");
	                System.err.println("Reason :" + e.getLocalizedMessage());
	            }
	        }
	    }

	    public void run() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
	        validate();
	        if( checkOptions() )
	            runOperation();
	    }
	    
	    public boolean checkOptions() {
	        boolean run = false;

	        if(all) {
	            // force operations to broadcast to ALL virtual machines.
	            vmName = null;
	            datacenter = null;
	            host = null;
	            System.out.println("Power operations will be broadcast to ALL virtual machines.");
	            run = true;
	        }
	        else if( vmName == null
	            && datacenter == null
	            && guestId == null
	            && host == null
	            && System.console() != null )
	        {
	            throw new IllegalStateException("You must specify one of --vmname or --datacentername or --hostname or --all");
	        }
	        else {
	            run = true;
	        }

	        return run;
	    }
}
