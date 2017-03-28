package vm.helper;

import com.vmware.vim25.*;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class VCLicensesInfo extends VCTaskBase {

    private static ManagedObjectReference licManagerRef = null;
    private static ManagedObjectReference licenseAssignmentManagerRef = null;
    private static List<LicenseAssignmentManagerLicenseAssignment> licenses;

    private static void initLicAssignmentManagerRef() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        licenseAssignmentManagerRef = (ManagedObjectReference) VCHelper.entityProps(licManagerRef,
                new String[]{"licenseAssignmentManager"}).get("licenseAssignmentManager");
    }

    private static void initLicenseAssignmentManagerLicenseAssignments() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        licenses = vimPort.queryAssignedLicenses(licenseAssignmentManagerRef, null);
    }

    private static String getInfo() {
        StringBuilder JsonInfo = new StringBuilder();
        JsonInfo.append("[");
        for (LicenseAssignmentManagerLicenseAssignment license : licenses) {
            LicenseManagerLicenseInfo assignedLicense = license.getAssignedLicense();
            //List<KeyAnyValue> propertys = license.getProperties();
            for (KeyAnyValue property : license.getProperties()) {
                if (property.getKey().equalsIgnoreCase("Evaluation")) {
                    LicenseManagerEvaluationInfo lsInfo = (LicenseManagerEvaluationInfo) property.getValue();
                    for (KeyAnyValue propertyOfDay : lsInfo.getProperties()) {
                        if (propertyOfDay.getKey().equalsIgnoreCase("expirationDate")) {
                            JsonInfo.append(String.format("{ \"Name of the Licnese\":%s, \"Key of the License\":%s, \"Date of the expiration\":%s}",
                                    assignedLicense.getName(), assignedLicense.getLicenseKey(), propertyOfDay.getValue()));
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

    public static String run() throws RuntimeFaultFaultMsg, LicenseEntityNotFoundFaultMsg, InvalidPropertyFaultMsg, KeyManagementException, NoSuchAlgorithmException, InvalidLoginFaultMsg, InvalidLocaleFaultMsg {
        if (!VCClientSession.IsConnected()) {
            VCClientSession.Connect();
        }
        init();
        if (serviceContent != null) {
            licManagerRef = serviceContent.getLicenseManager();
        }
        initLicAssignmentManagerRef();
        initLicenseAssignmentManagerLicenseAssignments();
        return getInfo();
    }
}
