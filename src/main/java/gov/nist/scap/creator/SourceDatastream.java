package gov.nist.scap.creator;

import gov.nist.scap.creator.enums.ComponentType;
import gov.nist.scap.creator.enums.UseCase;
import gov.nist.scap.creator.helpers.ComponentOrderComparator;
import gov.nist.scap.creator.helpers.ScapNamespaceContext;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SourceDatastream {

    // The format for the XML timestamp
    private static SimpleDateFormat sdf = new SimpleDateFormat(
        "yyyy-MM-dd'T'hh:mm:ss");

    private static DocumentBuilder db;
    private static String scapNs =
        "http://scap.nist.gov/schema/scap/source/1.2";
    private static String xsiNs =
        "http://www.w3.org/2001/XMLSchema-instance";
    private static String xlinkNs =
        "http://www.w3.org/1999/xlink";
    private static String catNs =
        "urn:oasis:names:tc:entity:xmlns:xml:catalog";

    // Holds the xpath to find the external dependencies in XCCDF
    private static XPathExpression xccdfDependXPath;
    // Holds the xpath to find the external dependencies in CPE Dictionary
    private static XPathExpression cpeDictDependXPath;

    static {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            db = dbf.newDocumentBuilder();

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            xpath.setNamespaceContext(new ScapNamespaceContext());
            xccdfDependXPath = xpath.compile("//xccdf:check-content-ref");
            cpeDictDependXPath = xpath.compile("//cpe-dict:check");
        } catch (ParserConfigurationException e) {
            // Should never happen
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            // Should never happen
            e.printStackTrace();
        }

    }

    public static void convertZip(
            File zipfile,
            UseCase uc,
            String idNamespace,
            OutputStream outputStream) throws IOException, XPathExpressionException, SAXException, TransformerException {
        // Set up the ZIP file
        ZipFile zf = new ZipFile(zipfile);
        //Get the zip file name minus the extension
        String zipFileName = zipfile.getName();
        // Get the ZIP entries
        Enumeration<? extends ZipEntry> zee = zf.entries();
        // Map the document name to a Document XML object; use the Comparator so
        // the components output is in the order specified in SCAP 1.0 and 1.1
        // (not necessary, but nice)
        Map<String, Document> map =
            new TreeMap<String, Document>(new ComponentOrderComparator());
        // Map component names to the dependencies associated with the map
        Map<String, Set<String>> dependencyMap =
            new HashMap<String, Set<String>>();

        // Create the output document
        Document outDoc = db.newDocument();
        outDoc.setXmlStandalone(true);
        Element root = outDoc.createElementNS(scapNs, "data-stream-collection");
        attachId(root, idNamespace, "collection", zipFileName);
        root.setAttribute("schematron-version", "1.0");
        root.setAttribute("xmlns:xsi", xsiNs);
        root.setAttribute("xmlns:xlink", xlinkNs);
        root.setAttribute("xmlns:cat", catNs);
        root.setAttribute(
            "xsi:schemaLocation",
            scapNs
                + " http://scap.nist.gov/schema/scap/1.2/scap-source-data-stream_1.2.xsd");
        outDoc.appendChild(root);

        // For each component file
        while (zee.hasMoreElements()) {
            ZipEntry ze = zee.nextElement();
            Document doc = db.parse(zf.getInputStream(ze));

            ComponentType componentType = ComponentType.getType(ze.getName());
            if (componentType == ComponentType.XCCDF) {
                doc = updateXCCDF(doc, idNamespace);
            }
            // Map the file name to the xml document
            map.put(ze.getName(), doc);
            dependencyMap.put(
                ze.getName(),
                getComponentDependencies(
                    (Element)doc.getFirstChild(),
                    componentType));
        }

        String xccdfName = null;
        List<String> checks = new ArrayList<String>();
        String dictionaryName = null;
        // Get the name of the XCCDF, checks, and Dictionaries
        for (String name : map.keySet()) {
            ComponentType te = ComponentType.getType(name);
            if (te != null) {
                switch (te) {
                case XCCDF:
                    xccdfName = name;
                    break;
                case OVAL:
                    checks.add(name);
                    break;
                case PATCHES:
                    checks.add(name);
                    break;
                case CPE_OVAL:
                    checks.add(name);
                    break;
                case CPE_DICT:
                    dictionaryName = name;
                    break;
                }
            }
        }

        // Map the component names to the generated Id (which should be globally
        // unique) for a component
        Map<String, String> mapComponentNamesToIds =
            new HashMap<String, String>();
        // Map the component names to the component-ref elements for that
        // component
        Map<String, Element> mapComponentNameToComponentRef =
            new HashMap<String, Element>();
        // Map the component names to the component-ref Ids
        Map<String, String> mapComponentNamesToLinkIds =
            new HashMap<String, String>();

        Element datastreamEle = outDoc.createElementNS(scapNs, "data-stream");
        root.appendChild(datastreamEle);
        attachId(datastreamEle, idNamespace, "datastream", zipFileName);
        datastreamEle.setAttribute("use-case", uc.toString());
        datastreamEle.setAttribute("scap-version", "1.2");
        datastreamEle.setAttribute("timestamp", sdf.format(new Date()));

        Element dictEle = outDoc.createElementNS(scapNs, "dictionaries");
        datastreamEle.appendChild(dictEle);
        Element ref = outDoc.createElementNS(scapNs, "component-ref");
        mapComponentNamesToLinkIds.put(dictionaryName, attachId(ref, idNamespace, "cref", dictionaryName));
        mapComponentNamesToIds.put(dictionaryName, generateId(idNamespace, "comp", dictionaryName));
        mapComponentNameToComponentRef.put(dictionaryName, ref);
        ref.setAttribute("xlink:href", "#" + mapComponentNamesToIds.get(dictionaryName));
        dictEle.appendChild(ref);

        Element checklistsEle = outDoc.createElementNS(scapNs, "checklists");
        datastreamEle.appendChild(checklistsEle);
        ref = outDoc.createElementNS(scapNs, "component-ref");
        mapComponentNamesToLinkIds.put(xccdfName, attachId(ref, idNamespace, "cref", xccdfName));
        mapComponentNamesToIds.put(xccdfName, generateId(idNamespace, "comp", xccdfName));
        mapComponentNameToComponentRef.put(xccdfName, ref);
        ref.setAttribute("xlink:href", "#" + mapComponentNamesToIds.get(xccdfName));
        checklistsEle.appendChild(ref);

        Element checksEle = outDoc.createElementNS(scapNs, "checks");
        datastreamEle.appendChild(checksEle);
        for (String check : checks) {
            ref = outDoc.createElementNS(scapNs, "component-ref");
            mapComponentNamesToLinkIds.put(check, attachId(ref, idNamespace, "cref", check));
            mapComponentNamesToIds.put(check, generateId(idNamespace, "comp", check));
            mapComponentNameToComponentRef.put(check, ref);
            ref.setAttribute("xlink:href", "#" + mapComponentNamesToIds.get(check));
            checksEle.appendChild(ref);
        }

        // For each component
        for (String component : dependencyMap.keySet()) {
            // For each dependencies of a component
            for (String dep : dependencyMap.get(component)) {
                Element derefMap = outDoc.createElementNS(catNs, "cat:uri");
                // If the dependency can't be found in the local bundle, then
                // specify the link as 'remote'
                if (mapComponentNamesToLinkIds.get(dep) == null) {
                    ref = outDoc.createElementNS(scapNs, "component-ref");
                    mapComponentNamesToLinkIds.put(dep, attachId(ref, idNamespace, "cref", "d"+dep.hashCode()));
                    mapComponentNameToComponentRef.put(dep, ref);
                    ref.setAttribute("xlink:href", dep);
                    checksEle.appendChild(ref);
                }

                derefMap.setAttribute("name", dep);
                derefMap.setAttribute(
                    "uri",
                    "#" + mapComponentNamesToLinkIds.get(dep));
                if( !mapComponentNameToComponentRef.get(component).getNodeName().equals("cat:catalog") ) {
                    Element e = outDoc.createElementNS(catNs, "cat:catalog");
                    mapComponentNameToComponentRef.get(component).appendChild(e);
                    mapComponentNameToComponentRef.put(component, e);
                }
                
                mapComponentNameToComponentRef.get(component).appendChild(
                    derefMap);

            }
        }

        // Output all of the components
        for (String name : map.keySet()) {
            Element componentEle = outDoc.createElementNS(scapNs, "component");
            componentEle.appendChild(outDoc.adoptNode(map.get(name).getFirstChild()));
            componentEle.setAttribute("id", mapComponentNamesToIds.get(name));
            componentEle.setAttribute("timestamp", sdf.format(new Date()));
            root.appendChild(componentEle);
        }

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();

        DOMSource source = new DOMSource(outDoc);
        StreamResult result = new StreamResult(outputStream);
        transformer.transform(source, result);
    }

    // Get the component dependencies by using the XPath specified at the top of
    // the file
    private static Set<String> getComponentDependencies(
            Element ele,
            ComponentType type) throws XPathExpressionException {
        if (type == ComponentType.XCCDF) {
            NodeList result =
                (NodeList)xccdfDependXPath.evaluate(ele, XPathConstants.NODESET);
            Set<String> returnSet = new HashSet<String>();
            for (int i = 0, size = result.getLength(); i < size; i++) {
                returnSet.add(((Element)result.item(i)).getAttribute("href"));
            }
            return returnSet;
        } else if (type == ComponentType.CPE_DICT) {
            NodeList result =
                (NodeList)cpeDictDependXPath.evaluate(
                    ele,
                    XPathConstants.NODESET);
            Set<String> returnSet = new HashSet<String>();
            for (int i = 0, size = result.getLength(); i < size; i++) {
                returnSet.add(((Element)result.item(i)).getAttribute("href"));
            }
            return returnSet;
        } else {
            return new HashSet<String>();
        }
    }

    // Generate an ID based on the globally unique ID guidance
    private static String generateId(String namespace, String type, String name) {
        return "scap_" + namespace + "_" + type + "_" + name;
    }

    // Attach a random ID to an element
    private static String attachId(Element ele, String namespace, String type, String name) {
        String id = generateId(namespace, type, name);
        ele.setAttribute("id", id);
        return id;
    }

    //Upgrade from XCCDF 1.1 to 1.2
    public static Document updateXCCDF(Document d, String namespace) throws XPathExpressionException, TransformerFactoryConfigurationError, TransformerException {
        Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(SourceDatastream.class.getResourceAsStream("/xccdf-converter.xsl")));
        t.setParameter("local_var_reverseDNSnamespace", namespace);
        t.setParameter("schema_location", "http://scap.nist.gov/schema/xccdf/1.2/xccdf_1.2.xsd");
        DOMResult result = new DOMResult();
        t.transform(new DOMSource(d), result);
        Document xDoc = (Document)result.getNode();
        ((Element)xDoc.getFirstChild()).removeAttribute("style");
        ((Element)xDoc.getFirstChild()).setAttribute("style", "SCAP_1.2");
        return xDoc;
    }


}
