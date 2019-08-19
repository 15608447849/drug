package util;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

public class XMLUtils {
    public static Document loadXML(String xml) {
        Document document = null;

        try {
            document = DocumentHelper.parseText(xml);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return document;
    }
}