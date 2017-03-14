package vm.helper;

import com.vmware.vim25.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by huxia on 2017/3/14.
 * 完成于当天晚上 BY 胡箫
 */
public class VCCloneVM {
    /**
     * @功能描述：从现有的虚拟机创建出一个模板，并且创建这个模板的多个克隆实例到目标datacenter中
     * @param datacenterName    datacenter的名字
     * @param vmPathName        虚拟机的清单路径
     * @param cloneName         克隆出虚拟机的名称
     */
    private static void CloneVM(String datacenterName, String vmPathName, String cloneName) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, NoSuchMethodException, IllegalAccessException, InvocationTargetException, TaskInProgressFaultMsg, InvalidDatastoreFaultMsg, InsufficientResourcesFaultFaultMsg, FileFaultFaultMsg, VmConfigFaultFaultMsg, InvalidStateFaultMsg, MigrationFaultFaultMsg, CustomizationFaultFaultMsg, InvalidCollectorVersionFaultMsg {
        VimPortType vimPort = VCClientSession.getVimPort();
        ServiceContent serviceContent = VCClientSession.getServiceContent();

        // 找到数据中心的对象引用
        ManagedObjectReference datacenterRef = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), datacenterName);
        if (datacenterRef == null) {
            System.out.printf("The specified datacenter [ %s ]is not found %n", datacenterName);
            return;
        }

        // 找到这个虚拟机的目录
        ManagedObjectReference vmFolderRef = (ManagedObjectReference) getDynamicProperty(datacenterRef, "vmFolder");
        if (vmFolderRef == null) {
            System.out.printf("The virtual machine is not found");
            return;
        }
        ManagedObjectReference vmRef = vimPort.findByInventoryPath(serviceContent.getSearchIndex(), vmPathName);
        if (vmRef == null) {
            System.out.printf("The VMPath specified [ %s ] is not found %n", vmPathName);
            return;
        }

        // 对克隆机器进行配置，cloneSpec标识配置信息
        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        cloneSpec.setLocation(relocateSpec);
        // 标识克隆完成后不开机
        cloneSpec.setPowerOn(false);
        // 标识克隆完成后不置成模板
        cloneSpec.setTemplate(false);

        System.out.printf("Cloning Virtual Machine [%s] to clone name [%s] %n", vmPathName.substring(vmPathName.lastIndexOf("/") + 1), cloneName);
        ManagedObjectReference cloneTask = vimPort.cloneVMTask(vmRef, vmFolderRef, cloneName, cloneSpec);
        if (getTaskResultAfterDone(cloneTask)) {
            System.out.printf("Successfully cloned Virtual Machine [%s] to clone name [%s] %n", vmPathName.substring(vmPathName.lastIndexOf("/") + 1), cloneName);
        } else {
            System.out.printf("Failure Cloning Virtual Machine [%s] to clone name [%s] %n", vmPathName.substring(vmPathName.lastIndexOf("/") + 1), cloneName);
        }
    }

    /**
     * @功能描述：此方法返回一个布尔值，指定任务是成功还是失败。
     * @param task 表示该任务的ManagedObjectReference
     * @return 表示任务结果的布尔值
     */
    private static boolean getTaskResultAfterDone(ManagedObjectReference task) throws InvalidCollectorVersionFaultMsg, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
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

    private static Object getDynamicProperty(ManagedObjectReference mor, String propertyName) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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
                    // hint：ArrayOfManagedObjectReference.getManagedObjectReference()返回ManagedObjectReference[]数组
                    if (methodExists(dynamicPropertyVal, "get" + methodName, null)) {
                        methodName = "get" + methodName;
                    } else {
                        // 对于基本数据类型，构造相应的methodname
                        // hint：对于ArrayOfInt，methodName 是 get_int
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
     * @功能描述：在向服务注册的属性收集器检索中单个对象的内容
     * @param mor           从这个ManagedObjectReference中获取内容
     * @param properties    要检索的对象的属性名称
     * @return 检索到的对象内容
     */
    private static ObjectContent[] getObjectProperties(ManagedObjectReference mor, String[] properties) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
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
     * @功能描述：使用新的RetrievePropertiesEx方法来模拟现在已弃用的RetrieveProperties方法
     * @param listpfs 属性过滤器列表
     * @return 对象内容列表
     */
    private static List<ObjectContent> retrievePropertiesAllObjects(List<PropertyFilterSpec> listpfs) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();

        List<ObjectContent> listobjcont = new ArrayList<>();

        ManagedObjectReference propCollectorRef = VCClientSession.getServiceContent().getPropertyCollector();
        RetrieveResult rslts = VCClientSession.getVimPort().retrievePropertiesEx(propCollectorRef, listpfs, propObjectRetrieveOpts);
        if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
            listobjcont.addAll(rslts.getObjects());
        }
        String token = null;
        if (rslts != null && rslts.getToken() != null) {
            token = rslts.getToken();
        }
        while (token != null && !token.isEmpty()) {
            rslts = VCClientSession.getVimPort().continueRetrievePropertiesEx(propCollectorRef, token);
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

    public static void run(String datacenterName, String vmPathName, String cloneName) throws InvalidLoginFaultMsg, NoSuchAlgorithmException, RuntimeFaultFaultMsg, InvalidLocaleFaultMsg, KeyManagementException, InsufficientResourcesFaultFaultMsg, InvocationTargetException, NoSuchMethodException, TaskInProgressFaultMsg, InvalidStateFaultMsg, IllegalAccessException, CustomizationFaultFaultMsg, FileFaultFaultMsg, MigrationFaultFaultMsg, InvalidPropertyFaultMsg, InvalidDatastoreFaultMsg, VmConfigFaultFaultMsg, InvalidCollectorVersionFaultMsg {
        if (!VCClientSession.IsConnected()) {
            VCClientSession.Connect();
        }

        CloneVM(datacenterName, vmPathName, cloneName);
    }
}
