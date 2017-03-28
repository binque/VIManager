package vm.helper;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidLocaleFaultMsg;
import com.vmware.vim25.InvalidLoginFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.KeyAnyValue;
import com.vmware.vim25.KeyValue;
import com.vmware.vim25.LicenseAssignmentManagerLicenseAssignment;
import com.vmware.vim25.LicenseEntityNotFoundFaultMsg;
import com.vmware.vim25.LicenseManagerEvaluationInfo;
import com.vmware.vim25.LicenseManagerLicenseInfo;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VimPortType;

public class VCLicensesInfo {
	private VimPortType vimPort;
 	private ServiceContent serviceContent;	 
 


    private ManagedObjectReference licManagerRef = null;
    private ManagedObjectReference licenseAssignmentManagerRef = null;
    private List<LicenseAssignmentManagerLicenseAssignment> licenses;

    public void initLicManagerRef() {
    	serviceContent = VCClientSession.getServiceContent();
    	vimPort = VCClientSession.getVimPort(); 
        if (serviceContent != null) {
            licManagerRef = serviceContent.getLicenseManager();
        }
    }

    public void initLicAssignmentManagerRef() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        licenseAssignmentManagerRef = (ManagedObjectReference) VCHelper.entityProps(licManagerRef,
                new String[] { "licenseAssignmentManager" }).get("licenseAssignmentManager");
    }

    public void initLicenseAssignmentManagerLicenseAssignments() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        licenses = vimPort.queryAssignedLicenses(licenseAssignmentManagerRef, null);
    }
    
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
    
    public String  getInfo(){
    	StringBuilder JsonInfo = new StringBuilder();
		JsonInfo.append("[");
    	for(LicenseAssignmentManagerLicenseAssignment license:licenses){
    		LicenseManagerLicenseInfo assignedLicense = license.getAssignedLicense();
    		List<KeyAnyValue> propertys =assignedLicense.getProperties();
    		//List<KeyAnyValue> propertys = license.getProperties();
    		for(KeyAnyValue property :license.getProperties()){
    			if (property.getKey().equalsIgnoreCase("Evaluation")) {
    				LicenseManagerEvaluationInfo lsInfo =  (LicenseManagerEvaluationInfo) property.getValue();
    				for(KeyAnyValue propertyOfDay :lsInfo.getProperties()){
    					if(propertyOfDay.getKey().equalsIgnoreCase("expirationDate")){
    						JsonInfo.append(String.format("{ \"Name of the Licnese\":%s, \"Key of the License\":%s, \"Date of the expiration\":%s}", 
    								assignedLicense.getName(),assignedLicense.getLicenseKey(),propertyOfDay.getValue()));
    						//System.out.println(assignedLicense.getName()+" "+assignedLicense.getLicenseKey()+" "+assignedLicense.getEditionKey()+" "+propertyOfDay.getKey()+" "+propertyOfDay.getValue());
    					}
    				}
    			}
    		}
    	}
    	JsonInfo.deleteCharAt(JsonInfo.length() - 2);
		JsonInfo.append("]");
		return JsonInfo.toString();	
    }
    
    public String run() throws RuntimeFaultFaultMsg, LicenseEntityNotFoundFaultMsg, InvalidPropertyFaultMsg, KeyManagementException, NoSuchAlgorithmException, InvalidLoginFaultMsg, InvalidLocaleFaultMsg {
    	 if (!VCClientSession.IsConnected()) {
             VCClientSession.Connect();
         }
        initLicManagerRef();
        initLicAssignmentManagerRef();
        initLicenseAssignmentManagerLicenseAssignments();
        return getInfo();
    }
}
