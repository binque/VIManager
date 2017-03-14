package vm.helper;

/*
 * Created by huxia on 2017/3/13.
 */

import com.vmware.vim25.*;

import javax.net.ssl.*;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @功能描述：客户程序的登陆、登出和认证
 */
public class ClientSession {
    private static final String url = "https://10.251.0.20:8000/sdk";
    private static final String username = "administrator@vsphere.local";
    private static final String password = "@Vmware123";

    private static final ManagedObjectReference SVC_INST_REF = new ManagedObjectReference();
    private static VimService vimService;
    public static VimService getVimService() {
        return vimService;
    }
    private static VimPortType vimPort;
    public static VimPortType getVimPort() {
        return vimPort;
    }

    private static ServiceContent serviceContent;
    public static ServiceContent getServiceContent() {
        return serviceContent;
    }
    private static final String SVC_INST_NAME = "ServiceInstance";
    private static boolean isConnected = false;
    private static ManagedObjectReference perfManager;
    public static ManagedObjectReference getPerfManager() {
        return perfManager;
    }
    private static ManagedObjectReference propCollectorRef;
    public static ManagedObjectReference getPropCollectorRef() {
        return propCollectorRef;
    }

    private static class TrustAllTrustManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(java.security.cert.X509Certificate[] certs)
        {
            return true;
        }
    }

    private static void trustAllHttpsCertificates() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new TrustAllTrustManager();
        trustAllCerts[0] = tm;
        SSLContext sc = SSLContext.getInstance("SSL");
        SSLSessionContext sslsc = sc.getServerSessionContext();
        sslsc.setSessionTimeout(0);
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    /**
     * @功能描述：连接认证
     */
    public static void Connect() throws RuntimeFaultFaultMsg, InvalidLoginFaultMsg, InvalidLocaleFaultMsg, KeyManagementException, NoSuchAlgorithmException {
        HostnameVerifier hv = (s, sslSession) -> true;
        trustAllHttpsCertificates();

        HttpsURLConnection.setDefaultHostnameVerifier(hv);

        SVC_INST_REF.setType(SVC_INST_NAME);
        SVC_INST_REF.setValue(SVC_INST_NAME);

        vimService = new VimService();
        vimPort = vimService.getVimPort();
        Map<String, Object> ctxt = ((BindingProvider)vimPort).getRequestContext();

        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        serviceContent = vimPort.retrieveServiceContent(SVC_INST_REF);
        vimPort.login(serviceContent.getSessionManager(), username, password, null);
        isConnected = true;

        perfManager = serviceContent.getPerfManager();
        propCollectorRef = serviceContent.getPropertyCollector();

        System.out.println(serviceContent.getAbout().getFullName());
        System.out.println("Server type is " + serviceContent.getAbout().getApiType());
    }

    /**
     * @功能描述：断开连接
     */
    public static void Disconnect() throws RuntimeFaultFaultMsg {
        if (isConnected){
            vimPort.logout(serviceContent.getSessionManager());
        }
        isConnected = false;
    }

    /**
     * @功能描述：打印错误信息
     */
    private static void printSoapFaultException(SOAPFaultException sfe) {
        System.out.println("Soap fault: ");
        if (sfe.getFault().hasDetail())
        {
            System.out.println(sfe.getFault().getDetail().getFirstChild().getLocalName());
        }
        if (sfe.getFault().getFaultString() != null)
        {
            System.out.println("Message: " + sfe.getFault().getFaultString());
        }
    }

    /**
     * @功能描述：根据属性检索要查询的对象信息
     * @param listpfs 属性过滤器集合
     */
    public static List<ObjectContent> RetrievePropertiesAllObjects(List<PropertyFilterSpec> listpfs) {
        RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();
        List<ObjectContent> listobjcontent = new ArrayList<>();
        try
        {
            RetrieveResult rslts = vimPort.retrievePropertiesEx(propCollectorRef, listpfs, propObjectRetrieveOpts);
            if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty())
            {
                listobjcontent.addAll(rslts.getObjects());
            }
            String token = null;
            if (rslts != null && rslts.getToken() != null)
            {
                token = rslts.getToken();
            }
            while (token != null && !token.isEmpty())
            {
                rslts = vimPort.continueRetrievePropertiesEx(propCollectorRef, token);
                token = null;
                if (rslts != null)
                {
                    token = rslts.getToken();
                    if (rslts.getObjects() != null && !rslts.getObjects().isEmpty())
                    {
                        listobjcontent.addAll(rslts.getObjects());
                    }
                }
            }
        }
        catch (SOAPFaultException sfe)
        {
            printSoapFaultException(sfe);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return listobjcontent;
    }
}
