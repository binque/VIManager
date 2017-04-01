package vm.helper;

import com.vmware.vim25.*;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by huxia on 2017/3/19.
 * Completed By Huxiao
 */
public class VCTaskBase {
    static VimPortType vimPort;
    protected static ServiceContent serviceContent;

    protected static Logger logger = Logger.getLogger(VCTaskBase.class);

    protected static void init() throws InvalidLoginFaultMsg, NoSuchAlgorithmException, RuntimeFaultFaultMsg, InvalidLocaleFaultMsg, KeyManagementException {
        VCClientSession.Connect();
        vimPort = VCClientSession.getVimPort();
        serviceContent = VCClientSession.getServiceContent();
    }

    /**
     * @param task 表示该任务的ManagedObjectReference
     * @return 表示任务结果的布尔值
     * @功能描述 此方法返回一个布尔值，指定任务是成功还是失败。
     */
    static boolean getTaskResultAfterDone(ManagedObjectReference task) throws InvalidCollectorVersionFaultMsg, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        boolean retValue = false;

        Object[] result = VCClientSession.WaitForValues(task, new String[]{"info.state", "info.error"}, new String[]{"state"},
                new Object[][]{new Object[]{TaskInfoState.SUCCESS, TaskInfoState.ERROR}});

        if (result[0].equals(TaskInfoState.SUCCESS)) {
            retValue = true;
        }
        if (result[1] instanceof LocalizedMethodFault) {
            throw new RuntimeException(
                    ((LocalizedMethodFault) result[1]).getLocalizedMessage());
        }
        return retValue;
    }

    static Object getDynamicProperty(ManagedObjectReference mor, String propertyName) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InvalidLoginFaultMsg, NoSuchAlgorithmException, InvalidLocaleFaultMsg, KeyManagementException {
        ObjectContent[] objContent = getObjectProperties(mor, new String[]{propertyName});

        Object propertyValue = null;
        if (objContent != null) {
            List<DynamicProperty> listdp = objContent[0].getPropSet();
            if (listdp != null) {
                // 检查ArrayOfXXX对象的动态属性
                Object dynamicPropertyVal = listdp.get(0).getVal();
                String dynamicPropertyName = dynamicPropertyVal.getClass().getName();
                if (dynamicPropertyName.contains("ArrayOf")) {
                    String methodName = dynamicPropertyName.substring(dynamicPropertyName.indexOf("ArrayOf") + "ArrayOf".length(), dynamicPropertyName.length());

                    // 如果对象是ArrayOfXXX对象，则通过在对象上调用getXXX()来获取XXX[]
                    // hint ArrayOfManagedObjectReference.getManagedObjectReference()返回ManagedObjectReference[]数组
                    if (methodExists(dynamicPropertyVal, "get" + methodName, null)) {
                        methodName = "get" + methodName;
                    } else {
                        // 对于基本数据类型，构造相应的methodname
                        // hint 对于ArrayOfInt，methodName 是 get_int
                        methodName = "get_" + methodName.toLowerCase();
                    }
                    Method getMorMethod = dynamicPropertyVal.getClass().getDeclaredMethod(methodName, (Class<?>[]) null);
                    propertyValue = getMorMethod.invoke(dynamicPropertyVal, (Object) null);
                } else if (dynamicPropertyVal.getClass().isArray()) {
                    // 处理反序列化的解包数组的情况
                    propertyValue = dynamicPropertyVal;
                } else {
                    propertyValue = dynamicPropertyVal;
                }
            }
        }
        return propertyValue;
    }

    private static boolean methodExists(Object obj, String methodName, Class[] parameterTypes) throws NoSuchMethodException {
        Method method = obj.getClass().getMethod(methodName, parameterTypes);
        return method != null;
    }

    /**
     * @param mor        从这个ManagedObjectReference中获取内容
     * @param properties 要检索的对象的属性名称
     * @return 检索到的对象内容
     * @功能描述 在向服务注册的属性收集器检索中单个对象的内容
     */
    private static ObjectContent[] getObjectProperties(ManagedObjectReference mor, String[] properties) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidLoginFaultMsg, NoSuchAlgorithmException, InvalidLocaleFaultMsg, KeyManagementException {
        if (mor == null) {
            return null;
        }

        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.getPropSet().add(new PropertySpec());
        if (properties == null || properties.length == 0) {
            spec.getPropSet().get(0).setAll(Boolean.TRUE);
        } else {
            spec.getPropSet().get(0).setAll(Boolean.FALSE);
        }
        spec.getPropSet().get(0).setType(mor.getType());
        assert properties != null;
        spec.getPropSet().get(0).getPathSet().addAll(Arrays.asList(properties));
        spec.getObjectSet().add(new ObjectSpec());
        spec.getObjectSet().get(0).setObj(mor);
        spec.getObjectSet().get(0).setSkip(Boolean.FALSE);
        List<PropertyFilterSpec> listpfs = new ArrayList<>(1);
        listpfs.add(spec);
        List<ObjectContent> listobjcont = retrievePropertiesAllObjects(listpfs);
        return listobjcont.toArray(new ObjectContent[listobjcont.size()]);
    }

    /**
     * @param listpfs 属性过滤器列表
     * @return 对象内容列表
     * @功能描述 使用新的RetrievePropertiesEx方法来模拟现在已弃用的RetrieveProperties方法
     */
    private static List<ObjectContent> retrievePropertiesAllObjects(List<PropertyFilterSpec> listpfs) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidLoginFaultMsg, NoSuchAlgorithmException, InvalidLocaleFaultMsg, KeyManagementException {
        init();

        RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();

        List<ObjectContent> listobjcont = new ArrayList<>();

        ManagedObjectReference propCollectorRef = serviceContent.getPropertyCollector();
        RetrieveResult rslts = vimPort.retrievePropertiesEx(propCollectorRef, listpfs, propObjectRetrieveOpts);
        if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
            listobjcont.addAll(rslts.getObjects());
        }
        String token = null;
        if (rslts != null && rslts.getToken() != null) {
            token = rslts.getToken();
        }
        while (token != null && !token.isEmpty()) {
            rslts = vimPort.continueRetrievePropertiesEx(propCollectorRef, token);
            token = null;
            if (rslts != null) {
                token = rslts.getToken();
                if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
                    listobjcont.addAll(rslts.getObjects());
                }
            }
        }
        return listobjcont;
    }

    protected static DatastoreSummary getDatastoreNameWithFreeSpace(ManagedObjectReference virtualMachine, int minFreeSpace)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        DatastoreSummary ds = null;
        List<ManagedObjectReference> datastores = ((ArrayOfManagedObjectReference) VCHelper.entityProps(virtualMachine, new String[]{"datastore"}).get("datastore")).getManagedObjectReference();
        for (ManagedObjectReference datastore : datastores) {
            ds = (DatastoreSummary) VCHelper.entityProps(datastore, new String[]{"summary"}).get("summary");
            if (ds.getFreeSpace() > minFreeSpace) {
                break;
            }
        }
        return ds;
    }
}
