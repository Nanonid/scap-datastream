package gov.nist.scap.creator.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public class CustomNamespaceContext implements NamespaceContext {

    private final Map<String, String> prefixNamespaceMap;
    private final Map<String, String> namespacePrefixMap =
        new HashMap<String, String>();

    public CustomNamespaceContext(Map<String, String> prefixNamespaceMap) {
        this.prefixNamespaceMap = prefixNamespaceMap;
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

    public Map<String, String> createPrefixNamespaceMap() {
        Map<String, String> returnMap = new HashMap<String, String>();
        for (String s : prefixNamespaceMap.keySet()) {
            returnMap.put(s, prefixNamespaceMap.get(s));
        }
        return returnMap;
    }

}
