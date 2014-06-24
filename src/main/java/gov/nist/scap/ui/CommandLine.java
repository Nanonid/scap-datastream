package gov.nist.scap.ui;

import gov.nist.scap.creator.SourceDatastream;
import gov.nist.scap.creator.enums.UseCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

public class CommandLine {

    public static void main(String[] arg0)
            throws XPathExpressionException, FileNotFoundException,
            IOException, SAXException, TransformerException {
        if (arg0.length < 3 || !arg0[0].endsWith(".zip")
            || !new File(arg0[0]).isFile() || UseCase.valueOf(arg0[1]) == null) {
            System.out.println("3 arguments MUST be provide.");
            System.out.println("Argument 1 MUST point to a SCAP data stream 1.0 ZIP bundle");
            System.out.println("Argument 2 MUST indicate the use case.  Allowed values: CONFIGURATION, VULNERABILITY_XCCDF_OVAL, OVAL_ONLY, SYSTEM_INVENTORY");
            System.out.println("Argument 3 MUST indicate the namespace to use when upgrading XCCDF IDs and creating SCAP IDs");
        } else {
            String namespace = arg0[2];
            File sourceFile = new File(arg0[0]);
            File outFile =
                new File(arg0[0].substring(0, arg0[0].length() - 3) + "xml");
            System.out.println("Output file: " + outFile.getAbsolutePath());
            if( outFile.exists() ) {
                System.out.println("Overwrite existing file (yes/no)?");
                byte[] b = new byte[10];
                System.in.read(b);
                if( !"yes".equals(new String(b).trim()) ) {
                    System.exit(0);
                } else {
                    if( !outFile.delete() ) {
                        System.err.println("File cannot be deleted");
                        System.exit(0);
                    }
                }
            }
            if (!outFile.createNewFile() || !outFile.canWrite()) {
                System.err.println("Output file cannot be written: "
                    + outFile.getAbsolutePath());
            }
            SourceDatastream.convertZip(
                sourceFile,
                UseCase.valueOf(arg0[1]), namespace,
                new FileOutputStream(outFile));
            System.out.println("Output file written: "
                + outFile.getAbsolutePath());
        }

    }
}
