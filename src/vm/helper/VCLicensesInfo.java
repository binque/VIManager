package vm.helper;

import com.vmware.vim25.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.List;

public class VCLicensesInfo extends VCTaskBase {

    private ManagedObjectReference licManagerRef = null;
    private ManagedObjectReference licenseAssignmentManagerRef = null;
    private List<LicenseAssignmentManagerLicenseAssignment> licenses;

    private void initLicAssignmentManagerRef() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        licenseAssignmentManagerRef = (ManagedObjectReference) VCHelper.entityProps(serviceContent, vimPort, licManagerRef, new String[]{"licenseAssignmentManager"}).get("licenseAssignmentManager");
    }

    private void initLicenseAssignmentManagerLicenseAssignments() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        licenses = vimPort.queryAssignedLicenses(licenseAssignmentManagerRef, null);
    }

    private String getInfo() {
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
                            //logger.info(assignedLicense.getName()+" "+assignedLicense.getLicenseKey()+" "+assignedLicense.getEditionKey()+" "+propertyOfDay.getKey()+" "+propertyOfDay.getValue());
                        }
                    }
                }
            }
        }
        return JsonInfo.toString();
    }

    public String run() throws Exception {
        try {
            init();
            if (serviceContent != null) {
                licManagerRef = serviceContent.getLicenseManager();
            }
            initLicAssignmentManagerRef();
            initLicenseAssignmentManagerLicenseAssignments();
            return getInfo();
        }catch (Throwable e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            vcClientSession.Disconnect();
        }

    }
}
