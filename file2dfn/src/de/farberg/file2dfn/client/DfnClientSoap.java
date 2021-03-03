package de.farberg.file2dfn.client;

import static de.farberg.file2dfn.helpers.Helper.readAsciiFile;

import java.io.File;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import de.dfncert.slight.SOAPClient;
import de.dfncert.slight.SOAPTraceable;
import de.dfncert.soap.DFNCERTPublic;
import de.dfncert.soap.DFNCERTRegistration;
import de.dfncert.soap.DFNCERTTypesExtendedObjectInfo;
import de.dfncert.tools.Cryptography;
import de.dfncert.tools.DFNPKIClient;
import de.farberg.file2dfn.helpers.CommandLineOptions;
import de.farberg.file2dfn.helpers.Helper;

public class DfnClientSoap implements DfnClient {
	private final static Logger log = Helper.getLogger(DfnClientSoap.class.getName());

	private DFNPKIClient client;
	private DFNCERTPublic publicClient = null;
	private DFNCERTRegistration registrationClient;
	private CommandLineOptions options;
	private int raId;
	private String password;
	private String caName;
	private String role;
	private String pin;

	public DfnClientSoap(CommandLineOptions options) throws Exception {
		this.options = options;

		String raIdFile = new File(options.configDir, "ra-id.txt").getCanonicalPath();
		String p12File = new File(options.configDir, "ca.p12").getCanonicalPath();
		String passwordFile = new File(options.configDir, "password.txt").getCanonicalPath();
		String caNameFile = new File(options.configDir, "ca-name.txt").getCanonicalPath();
		String roleFile = new File(options.configDir, "role.txt").getCanonicalPath();
		String pinFile = new File(options.configDir, "pin.txt").getCanonicalPath();

		this.raId = Integer.parseInt(readAsciiFile(raIdFile));
		this.caName = readAsciiFile(caNameFile);
		this.password = readAsciiFile(passwordFile);
		this.pin = readAsciiFile(pinFile);
		this.role = readAsciiFile(roleFile);

		this.client = new DFNPKIClient(this.caName);
		this.client.loadRAFromPKCS12(p12File, this.password.toCharArray());

		this.publicClient = client.getPublic();
		this.registrationClient = client.getRegistration();

		SOAPClient.setDebug(true);
		SOAPClient.setTraceable(new SOAPTraceable() {
			// method to convert Document to String
			public String doc2string(Document doc) {
				try {
					DOMSource domSource = new DOMSource(doc);
					StringWriter writer = new StringWriter();
					StreamResult result = new StreamResult(writer);
					TransformerFactory tf = TransformerFactory.newInstance();
					Transformer transformer = tf.newTransformer();
					transformer.transform(domSource, result);
					return writer.toString();
				} catch (TransformerException ex) {
					ex.printStackTrace();
					return null;
				}
			}

			@Override
			public void traceSOAPError(HttpsURLConnection conn, String msg, Document doc, Exception e) {
				String m ="traceSOAPError: " + msg + "(" + e + ")\n" + doc2string(doc); 
				log.info(m.replaceAll("\n",""));
			}

			@Override
			public void traceSOAPReceived(HttpsURLConnection conn, String msg, Document doc) {
				String m ="traceSOAPReceived: " + msg + "\n" + doc2string(doc); 
				log.info(m.replaceAll("\n",""));
			}

			@Override
			public void traceSOAPSent(HttpsURLConnection conn, String msg, Document doc) {
				String m ="traceSOAPSent: " + msg + "\n" + doc2string(doc); 
				log.info(m.replaceAll("\n",""));
			}
		});
	}

	public DFNPKIClient get() {
		return client;
	}

	@Override
	public int createRequest(String PKCS10, String[] AltNames, String AddName, String AddEMail, String AddOrgUnit, String subject) throws Exception {

		log.info("Invoking SOAP API::newRequest with subject = " + subject);
		return publicClient.newRequest(raId, PKCS10, AltNames, role, pin, AddName, AddEMail, AddOrgUnit, options.publish, subject);
	}

	@Override
	public boolean approveRequest(int serialNumber) throws Exception {
		byte rawRequestToApprove[] = registrationClient.getRawRequest(serialNumber);
		String pkcs7Signed = Cryptography.createPKCS7Signed(rawRequestToApprove, client.getRAPrivateKey(), client.getRACertificate());

		return registrationClient.approveRequest(serialNumber, rawRequestToApprove, pkcs7Signed);
	}

	@Override
	public String getCertificate(int serialNumber) throws Exception {
		String certificatePEM = registrationClient.getCertificateByRequestSerial(serialNumber);

		if (certificatePEM != "") {
			log.info("DFN returned non-empty reply: \n" + certificatePEM);

			// Parse certificate to ensure its valid
			return certificatePEM;
		}

		return null;
	}

	public int getRaId() {
		return raId;
	}

	public DFNCERTTypesExtendedObjectInfo[] getCertificates(BigInteger lastSerial, int limit) throws Exception {
		return registrationClient.searchItems2("certificate", "VALID", role, raId, lastSerial, limit);
	}

}
