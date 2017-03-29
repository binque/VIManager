package vm.helper;

import com.vmware.vim25.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class VCLicensesInfo extends VCTaskBase {

    private static ManagedObjectReference licManagerRef = null;
    private static ManagedObjectReference licenseAssignmentManagerRef = null;
    private static List<LicenseAssignmentManagerLicenseAssignment> licenses;

    private static void initLicAssignmentManagerRef() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        licenseAssignmentManagerRef = (ManagedObjectReference) VCHelper.entityProps(licManagerRef, new String[]{"licenseAssignmentManager"}).get("licenseAssignmentManager");
    }

    private static void initLicenseAssignmentManagerLicenseAssignments() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        licenses = vimPort.queryAssignedLicenses(licenseAssignmentManagerRef, null);
    }

    private static String getInfo() {
        JSONArray JsonInfo = new JSONArray();
        for (LicenseAssignmentManagerLicenseAssignment license : licenses) {
            LicenseManagerLicenseInfo assignedLicense = license.getAssignedLicense();
            //List<KeyAnyValue> propertys = license.getProperties();
            for (KeyAnyValue property : license.getProperties()) {
                if (property.getKey().equalsIgnoreCase("Evaluation")) {
                    LicenseManagerEvaluationInfo lsInfo = (LicenseManagerEvaluationInfo) property.getValue();
                    for (KeyAnyValue propertyOfDay : lsInfo.getProperties()) {
                        if (propertyOfDay.getKey().equalsIgnoreCase("expirationDate")) {
                            JSONObject jo = new JSONObject();
                            jo.put("Name of the Licnese", assignedLicense.getName());
                            jo.put("Key of the License", assignedLicense.getLicenseKey());
                            jo.put("Date of the expiration", propertyOfDay.getValue().toString());
                            JsonInfo.add(jo);
                            //logger.debug(assignedLicense.getName()+" "+assignedLicense.getLicenseKey()+" "+assignedLicense.getEditionKey()+" "+propertyOfDay.getKey()+" "+propertyOfDay.getValue());
                        }
                    }
                }
            }
        }
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
