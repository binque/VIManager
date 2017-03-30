package vm.helper;

import com.vmware.vim25.*;

import java.util.*;

/**
 * Created by huxia on 2017/3/15.
 * Completed By Huxiao
 */
class VCHelper {

    /**
     * @param container 所搜寻的容器
     * @param morefType 要过滤的类型
     * @return 过滤的结果
     * @功能描述 返回在属性列表中过滤的所提供容器的原始RetrieveResult对象
     */
    private static RetrieveResult containerViewByType(
            final ManagedObjectReference container,
            final String morefType,
            final RetrieveOptions retrieveOptions,
            final String... morefProperties
    ) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {

        PropertyFilterSpec[] propertyFilterSpecs = propertyFilterSpecs(container, morefType, morefProperties);
        return containerViewByType(container, morefType, morefProperties, retrieveOptions, propertyFilterSpecs);
    }

    private static PropertyFilterSpec[] propertyFilterSpecs(ManagedObjectReference container, String morefType, String[] morefProperties) throws RuntimeFaultFaultMsg {

        ManagedObjectReference viewManager = VCClientSession.getServiceContent().getViewManager();
        ManagedObjectReference containerView = VCClientSession.getVimPort().createContainerView(viewManager, container, Collections.singletonList(morefType), true);

        return new PropertyFilterSpec[]{
                new PropertyFilterSpecBuilder()
                        .propSet(
                                new PropertySpecBuilder()
                                        .all(Boolean.FALSE)
                                        .type(morefType)
                                        .pathSet(morefProperties)
                        )
                        .objectSet(
                        new ObjectSpecBuilder()
                                .obj(containerView)
                                .skip(Boolean.TRUE)
                                .selectSet(new TraversalSpecBuilder()
                                        .name("view")
                                        .path("view")
                                        .skip(Boolean.FALSE)
                                        .type("ContainerView")
                                )
                )
        };
    }

    private static RetrieveResult containerViewByType(
            final ManagedObjectReference container,
            final String morType,
            final String[] morPropertie,
            final RetrieveOptions retrieveOptions,
            final PropertyFilterSpec... propertyFilterSpecs
    ) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        return VCClientSession.getVimPort().retrievePropertiesEx(VCClientSession.getServiceContent().getPropertyCollector(),
                Arrays.asList(propertyFilterSpecs),
                retrieveOptions);
    }

    /**
     * @param container       指定容器，搜寻的起始点
     * @param morefType       指定类型
     * @param morefProperties 要为moref获取的属性数组
     * @return 映射的MOREF和映射的名称值对的属性请求的托管对象存在。 如果不存在，则返回空映射
     * @功能描述 返回指定容器下面指定类型的所有MOREF
     */
    static Map<ManagedObjectReference, Map<String, Object>> inContainerByType(ManagedObjectReference container, String morefType,
                                                                              String[] morefProperties, RetrieveOptions retrieveOptions) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        List<ObjectContent> listcont = containerViewByType(container, morefType, retrieveOptions, morefProperties).getObjects();

        Map<ManagedObjectReference, Map<String, Object>> tgetMoref = new HashMap<>();

        if (listcont != null) {
            for (ObjectContent objectContent : listcont) {
                Map<String, Object> propMap = new HashMap<>();
                List<DynamicProperty> listdps = objectContent.getPropSet();
                if (listdps != null) {
                    for (DynamicProperty dynamicProperty : listdps) {
                        propMap.put(dynamicProperty.getName(), dynamicProperty.getVal());
                    }
                }
                tgetMoref.put(objectContent.getObj(), propMap);
            }
        }
        return tgetMoref;
    }

    /**
     * @param folder    指定容器，搜寻的起始点
     * @param morefType 指定类型
     * @return 映射的MOREF和映射的名称值对的属性请求的托管对象存在。 如果不存在，则返回空映射
     * @功能描述 返回指定容器下面指定类型的所有MOREF
     */
    private static Map<String, ManagedObjectReference> inContainerByType(ManagedObjectReference folder, String morefType, RetrieveOptions retrieveOptions) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        RetrieveResult retrieveResult = containerViewByType(folder, morefType, retrieveOptions);
        return toMap(retrieveResult);
    }

    private static RetrieveResult containerViewByType(
            final ManagedObjectReference container,
            final String morefType,
            final RetrieveOptions retrieveOptions
    ) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        return containerViewByType(container, morefType, retrieveOptions, "name");
    }

    private static Map<String, ManagedObjectReference> toMap(RetrieveResult retrieveResult) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        final Map<String, ManagedObjectReference> tgetMoref = new HashMap<>();
        String token;

        token = populate(retrieveResult, tgetMoref);

        while (token != null && !token.isEmpty()) {
            // 基于新token获取结果
            retrieveResult = VCClientSession.getVimPort().continueRetrievePropertiesEx(VCClientSession.getServiceContent().getPropertyCollector(), token);
            token = populate(retrieveResult, tgetMoref);
        }
        return tgetMoref;
    }

    private static String populate(final RetrieveResult retrieveResult, final Map<String, ManagedObjectReference> tgetMoref) {
        String token = null;
        if (retrieveResult != null) {
            token = retrieveResult.getToken();
            for (ObjectContent oc : retrieveResult.getObjects()) {
                ManagedObjectReference mr = oc.getObj();
                String entityNm = null;
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        entityNm = (String) dp.getVal();
                    }
                }
                tgetMoref.put(entityNm, mr);
            }
        }
        return token;
    }

    private static String populate(final RetrieveResult rslts, final List<ObjectContent> listobjcontent) {
        String token = null;
        if (rslts != null) {
            token = rslts.getToken();
            listobjcontent.addAll(rslts.getObjects());
        }
        return token;
    }

    static Map<String, ManagedObjectReference> inContainerByType(ManagedObjectReference container, String morefType) throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
        return inContainerByType(container, morefType, new RetrieveOptions());
    }

    /**
     * @param entityMor 所要检索的实体的MOR
     * @param props     检索的属性集合
     * @return 返回属性名和其相应属性的Map
     * @throws InvalidPropertyFaultMsg 该属性不存在
     * @功能描述 检索当前MOR的属性
     */
    static Map<String, Object> entityProps(ManagedObjectReference entityMor, String[] props)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        final HashMap<String, Object> retVal = new HashMap<>();

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
        PropertyFilterSpec[] propertyFilterSpecs = {
                new PropertyFilterSpecBuilder()
                        .propSet(
                                // Create Property Spec
                                new PropertySpecBuilder()
                                        .all(Boolean.FALSE)
                                        .type(entityMor.getType())
                                        .pathSet(props)
                        )
                        .objectSet(
                        // Now create Object Spec
                        new ObjectSpecBuilder()
                                .obj(entityMor)
                )
        };

        List<ObjectContent> oCont =
                VCClientSession.getVimPort().retrievePropertiesEx(VCClientSession.getServiceContent().getPropertyCollector(),
                        Arrays.asList(propertyFilterSpecs), new RetrieveOptions()).getObjects();

        if (oCont != null) {
            for (ObjectContent oc : oCont) {
                List<DynamicProperty> dps = oc.getPropSet();
                for (DynamicProperty dp : dps) {
                    retVal.put(dp.getName(), dp.getVal());
                }
            }
        }
        return retVal;
    }

    static Map<ManagedObjectReference, Map<String, Object>> entityProps(
            List<ManagedObjectReference> entityMors, String[] props)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        Map<ManagedObjectReference, Map<String, Object>> retVal = new HashMap<>();
        // Create PropertyFilterSpec
        PropertyFilterSpecBuilder propertyFilterSpec = new PropertyFilterSpecBuilder();
        Map<String, String> typesCovered = new HashMap<>();

        for (ManagedObjectReference mor : entityMors) {
            if (!typesCovered.containsKey(mor.getType())) {
                // Create & add new property Spec
                propertyFilterSpec.propSet(
                        new PropertySpecBuilder()
                                .all(Boolean.FALSE)
                                .type(mor.getType())
                                .pathSet(props)
                );
                typesCovered.put(mor.getType(), "");
            }
            // Now create & add Object Spec
            propertyFilterSpec.objectSet(
                    new ObjectSpecBuilder().obj(mor)
            );
        }
        List<PropertyFilterSpec> propertyFilterSpecs =
                new ArrayList<>();
        propertyFilterSpecs.add(propertyFilterSpec);

        RetrieveResult rslts =
                VCClientSession.getVimPort().retrievePropertiesEx(VCClientSession.getServiceContent().getPropertyCollector(),
                        propertyFilterSpecs, new RetrieveOptions());

        List<ObjectContent> listobjcontent = new ArrayList<>();
        String token = populate(rslts, listobjcontent);
        while (token != null && !token.isEmpty()) {
            rslts =
                    VCClientSession.getVimPort().continueRetrievePropertiesEx(
                            VCClientSession.getServiceContent().getPropertyCollector(), token);

            token = populate(rslts, listobjcontent);
        }

        for (ObjectContent oc : listobjcontent) {
            List<DynamicProperty> dps = oc.getPropSet();
            Map<String, Object> propMap = new HashMap<>();
            if (dps != null) {
                for (DynamicProperty dp : dps) {
                    propMap.put(dp.getName(), dp.getVal());
                }
            }
            retVal.put(oc.getObj(), propMap);
        }
        return retVal;
    }

    /**
     * @param vmname           虚拟机的名称
     * @param propCollectorRef prop collector reference
     * @return 该虚拟机的MOR
     * @功能描述 通过虚拟机的名称获得该虚拟机的MOR
     */
    static ManagedObjectReference vmByVmname(final String vmname, final ManagedObjectReference propCollectorRef) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        ManagedObjectReference retVal = null;
        ManagedObjectReference rootFolder = VCClientSession.getServiceContent().getRootFolder();
        TraversalSpec tSpec = getVMTraversalSpec();
        // Create Property Spec
        PropertySpec propertySpec = new PropertySpecBuilder()
                .all(Boolean.FALSE)
                .pathSet("name")
                .type("VirtualMachine");

        // Now create Object Spec
        ObjectSpec objectSpec = new ObjectSpecBuilder()
                .obj(rootFolder)
                .skip(Boolean.TRUE)
                .selectSet(tSpec);

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
        // created above.
        PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpecBuilder()
                .propSet(propertySpec)
                .objectSet(objectSpec);

        List<PropertyFilterSpec> listpfs =
                new ArrayList<>(1);
        listpfs.add(propertyFilterSpec);

        RetrieveOptions options = new RetrieveOptions();
        List<ObjectContent> listobcont =
                VCClientSession.getVimPort().retrievePropertiesEx(propCollectorRef, listpfs, options).getObjects();

        if (listobcont != null) {
            for (ObjectContent oc : listobcont) {
                ManagedObjectReference mr = oc.getObj();
                String vmnm = null;
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        vmnm = (String) dp.getVal();
                    }
                }
                if (vmnm != null && vmnm.equals(vmname)) {
                    retVal = mr;
                    break;
                }
            }
        }
        return retVal;
    }

    /**
     * @return TraversalSpec规范获取到虚拟机管理对象
     */
    private static TraversalSpec getVMTraversalSpec() {
        // Create a traversal spec that starts from the 'root' objects
        // and traverses the inventory tree to get to the VirtualMachines.
        // Build the traversal specs bottoms up

        //Traversal to get to the VM in a VApp
        TraversalSpec vAppToVM = new TraversalSpecBuilder()
                .name("vAppToVM")
                .type("VirtualApp")
                .path("vm");

        //Traversal spec for VApp to VApp
        TraversalSpec vAppToVApp = new TraversalSpecBuilder()
                .name("vAppToVApp")
                .type("VirtualApp")
                .path("resourcePool")
                .selectSet(
                        //SelectionSpec for both VApp to VApp and VApp to VM
                        new SelectionSpecBuilder().name("vAppToVApp"),
                        new SelectionSpecBuilder().name("vAppToVM")
                );


        //This SelectionSpec is used for recursion for Folder recursion
        SelectionSpec visitFolders = new SelectionSpecBuilder().name("VisitFolders");

        // Traversal to get to the vmFolder from DataCenter
        TraversalSpec dataCenterToVMFolder = new TraversalSpecBuilder()
                .name("DataCenterToVMFolder")
                .type("Datacenter")
                .path("vmFolder")
                .skip(false)
                .selectSet(visitFolders);

        // TraversalSpec to get to the DataCenter from rootFolder
        return new TraversalSpecBuilder()
                .name("VisitFolders")
                .type("Folder")
                .path("childEntity")
                .skip(false)
                .selectSet(
                        visitFolders,
                        dataCenterToVMFolder,
                        vAppToVM,
                        vAppToVApp
                );
    }

    public static class PropertyFilterSpecBuilder extends PropertyFilterSpec {
        private void init() {
            if (propSet == null) {
                propSet = new ArrayList<>();
            }
            if (objectSet == null) {
                objectSet = new ArrayList<>();
            }
        }

        public PropertyFilterSpecBuilder reportMissingObjectsInResults(final Boolean value) {
            this.setReportMissingObjectsInResults(value);
            return this;
        }

        PropertyFilterSpecBuilder propSet(final PropertySpec... propertySpecs) {
            init();
            this.propSet.addAll(Arrays.asList(propertySpecs));
            return this;
        }

        PropertyFilterSpecBuilder objectSet(final ObjectSpec... objectSpecs) {
            init();
            this.objectSet.addAll(Arrays.asList(objectSpecs));
            return this;
        }
    }

    public static class PropertySpecBuilder extends PropertySpec {
        private void init() {
            if (pathSet == null) {
                pathSet = new ArrayList<>();
            }
        }

        PropertySpecBuilder all(final Boolean all) {
            this.setAll(all);
            return this;
        }

        PropertySpecBuilder type(final String type) {
            this.setType(type);
            return this;
        }

        PropertySpecBuilder pathSet(final String... paths) {
            init();
            this.pathSet.addAll(Arrays.asList(paths));
            return this;
        }

        public PropertySpecBuilder addToPathSet(final Collection<String> paths) {
            init();
            this.pathSet.addAll(paths);
            return this;
        }
    }

    static class ObjectSpecBuilder extends ObjectSpec {
        private void init() {
            if (selectSet == null) {
                selectSet = new ArrayList<>();
            }
        }

        ObjectSpecBuilder obj(final ManagedObjectReference objectReference) {
            this.setObj(objectReference);
            return this;
        }

        ObjectSpecBuilder skip(final Boolean skip) {
            this.setSkip(skip);
            return this;
        }

        ObjectSpecBuilder selectSet(final SelectionSpec... selectionSpecs) {
            init();
            this.selectSet.addAll(Arrays.asList(selectionSpecs));
            return this;
        }
    }

    static class TraversalSpecBuilder extends TraversalSpec {
        private void init() {
            if (selectSet == null) {
                selectSet = new ArrayList<>();
            }
        }

        TraversalSpecBuilder name(final String name) {
            this.setName(name);
            return this;
        }

        TraversalSpecBuilder path(final String path) {
            this.setPath(path);
            return this;
        }

        TraversalSpecBuilder skip(final Boolean skip) {
            this.setSkip(skip);
            return this;
        }

        TraversalSpecBuilder type(final String type) {
            this.setType(type);
            return this;
        }

        TraversalSpecBuilder selectSet(final SelectionSpec... selectionSpecs) {
            init();
            this.selectSet.addAll(Arrays.asList(selectionSpecs));
            return this;
        }
    }

    static class SelectionSpecBuilder extends SelectionSpec {
        SelectionSpecBuilder name(final String name) {
            this.setName(name);
            return this;
        }
    }
}
