package de.farberg.file2dfn.client;

import static de.farberg.file2dfn.helpers.Helper.readAsciiFile;

import java.io.File;
import java.math.BigInteger;
import java.util.logging.Logger;

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
	}

	public DFNPKIClient get() {
		return client;
	}

	@Override
	public int createRequest(String PKCS10, String[] AltNames, String AddName, String AddEMail, String AddOrgUnit)
			throws Exception {

		return publicClient.newRequest(raId, PKCS10, AltNames, role, pin, AddName, AddEMail, AddOrgUnit,
				options.publish);
	}

	@Override
	public boolean approveRequest(int serialNumber) throws Exception {
		byte rawRequestToApprove[] = registrationClient.getRawRequest(serialNumber);
		String pkcs7Signed = Cryptography.createPKCS7Signed(rawRequestToApprove, client.getRAPrivateKey(),
				client.getRACertificate());

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
