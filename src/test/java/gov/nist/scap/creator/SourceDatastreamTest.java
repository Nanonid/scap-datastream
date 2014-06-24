package gov.nist.scap.creator;

import gov.nist.scap.creator.enums.UseCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.xml.sax.SAXException;

public class SourceDatastreamTest {


    @Test
    public void testFDCCIE7() throws XPathExpressionException, FileNotFoundException, IOException, SAXException, TransformerException {
        File sourceFile = new File("src/test/resources/fdcc-ie7.zip");
        File outFile = new File("target/test-classes/fdcc-ie7.xml");
        SourceDatastream.convertZip(sourceFile, UseCase.CONFIGURATION, "gov.nist.scap", new FileOutputStream(outFile));
    }
}
