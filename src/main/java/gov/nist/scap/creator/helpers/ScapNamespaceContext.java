package gov.nist.scap.creator.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public class ScapNamespaceContext implements NamespaceContext {

    private static final Map<String, String> prefixNamespaceMap =
        new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put("ds", "http://scap.nist.gov/schema/scap/source/1.2");
                put("xccdf", "http://checklists.nist.gov/xccdf/1.2");
                put("cpe-dict", "http://cpe.mitre.org/dictionary/2.0");
            }
        };

    private static final Map<String, String> namespacePrefixMap =
        new HashMap<String, String>();
    static {
        for (String key : prefixNamespaceMap.keySet()) {
            namespacePrefixMap.put(prefixNamespaceMap.get(key), key);
        }
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return Collections.singleton(namespacePrefixMap.get(namespaceURI)).iterator();
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return namespacePrefixMap.get(namespaceURI);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return prefixNamespaceMap.get(prefix);
    }

    public static Map<String, String> createPrefixNamespaceMap() {
        Map<String, String> returnMap = new HashMap<String, String>();
        for (String s : prefixNamespaceMap.keySet()) {
            returnMap.put(s, prefixNamespaceMap.get(s));
        }
        return returnMap;
    }

    public static Map<String, String> createPrefixNamespaceMap(String... prefixes) {
        Map<String, String> returnMap = new HashMap<String, String>();
        for (String s : prefixes) {
            if( prefixNamespaceMap.containsKey(s) )
                returnMap.put(s, prefixNamespaceMap.get(s));
        }
        return returnMap;
    }

}
