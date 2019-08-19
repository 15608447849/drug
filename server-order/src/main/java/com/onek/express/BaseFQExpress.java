package com.onek.express;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hyrdpf.util.LogUtil;
import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import util.http.HttpUtil;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class BaseFQExpress {
    @PropertiesFilePath("/express.properties")
    protected static class ProPerties extends ApplicationPropertiesBase {
        @PropertiesName("fq.clientCode")
        protected String clientCode;
        @PropertiesName("fq.custid")
        protected String custid;
        @PropertiesName("fq.api")
        protected String api;
        @PropertiesName("fq.checkWord")
        protected String checkWord;

        @Override
        public String toString() {
            return "ProPerties{" +
                    "clientCode='" + clientCode + '\'' +
                    ", custid='" + custid + '\'' +
                    ", api='" + api + '\'' +
                    ", checkWord='" + checkWord + '\'' +
                    '}';
        }
    }

    protected static final ProPerties INNER_PROPERTIES = new ProPerties();

    protected String lang = "zh-CN";

    protected abstract String getService();

    protected abstract Map<String, String> genRequstParams();
    protected abstract Map<String, String> genRequstParams(String code);

    protected String genXMLParams() {
        Document document = DocumentHelper.createDocument();
        Element root = DocumentHelper.createElement("Request");
        document.setRootElement(root);
        root.addAttribute("service", getService());
        root.addAttribute("lang", getLang());
        Element head = root.addElement("Head");
        head.setText(INNER_PROPERTIES.clientCode);

        Element body = root.addElement("Body");
        Element routeReq = body.addElement("RouteRequest");
        addAttrFromParams(routeReq, genRequstParams());

        return root.asXML();
    }

    protected String genXMLParams(String code) {
        Document document = DocumentHelper.createDocument();
        Element root = DocumentHelper.createElement("Request");
        document.setRootElement(root);
        root.addAttribute("service", getService());
        root.addAttribute("lang", getLang());
        Element head = root.addElement("Head");
        head.setText(INNER_PROPERTIES.clientCode);

        Element body = root.addElement("Body");
        Element routeReq = body.addElement("RouteRequest");
        addAttrFromParams(routeReq, genRequstParams(code));

        return root.asXML();
    }

    protected String genVerifyCode(String xml) {
        return md5EncryptAndBase64(xml + INNER_PROPERTIES.checkWord);
    }

    protected String httpCall(String xml, String verifyCode) {
        String result = "";

        Map<String, String> params = new HashMap<>();
        params.put("xml", xml);
        params.put("verifyCode", verifyCode);

        LogUtil.getDefaultLogger().info("verifyCode -> " + verifyCode);
        LogUtil.getDefaultLogger().info("xml -> " + xml);
        LogUtil.getDefaultLogger().info("INNER_PROPERTIES -> " + INNER_PROPERTIES);


        try {
            result = HttpUtil.formText(INNER_PROPERTIES.api, "POST", params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public String getResult(String code) {
        String xml = genXMLParams(code);

        return httpCall(xml, genVerifyCode(xml));
    }

    public String getResult() {
        String xml = genXMLParams();

        return httpCall(xml, genVerifyCode(xml));
    }

    private void addAttrFromParams(Element element, Map<String, String> params) {
        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();

        Map.Entry<String, String> next;
        while (it.hasNext()) {
            next = it.next();
            element.addAttribute(next.getKey(), next.getValue());
        }
    }

    private String md5EncryptAndBase64(String str) {
        return encodeBase64(md5Encrypt(str));
    }

    private byte[] md5Encrypt(String encryptStr) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(encryptStr.getBytes("utf8"));
            return md5.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String encodeBase64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    protected String getLang() {
        return lang;
    }

    protected void setLang(String lang) {
        this.lang = lang;
    }
}
