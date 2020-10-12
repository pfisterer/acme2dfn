package de.farberg.file2dfn;

import de.dfncert.soap.DFNCERTPublic;
import de.dfncert.soap.DFNCERTRegistration;
import de.dfncert.tools.DFNPKIClient;
import static de.farberg.file2dfn.Helper.readAsciiFile;

public class DfnClient {
	private DFNPKIClient client;
	private DFNCERTPublic publicClient = null;
	private DFNCERTRegistration registrationClient;
	private CommandLineOptions options;
	private int raId;
	private String password;
	private String caName;
	private String role;
	private String pin;

	public DfnClient(CommandLineOptions options) throws Exception {
		this.options = options;
		this.raId = Integer.parseInt(readAsciiFile(options.raIdFile));
		this.caName = readAsciiFile(options.caNameFile);
		this.password = readAsciiFile(options.passwordFile);
		this.pin = readAsciiFile(options.pinFile);
		this.role = readAsciiFile(options.roleFile);

		this.client = new DFNPKIClient(this.caName);
		this.client.loadRAFromPKCS12(options.p12File, this.password.toCharArray());

		this.publicClient = client.getPublic();
		this.registrationClient = client.getRegistration();
	}

	public DFNPKIClient get() {
		return client;
	} 
	public DFNCERTPublic getPublicClient() {
		return publicClient;
	}

	public DFNCERTRegistration getRegistrationClient() {
		return registrationClient;
	}

	/**
	 * Copied from the documentation:
	 * 
	 * @param PKCS10     xsd:string Der Zertifikatantrag im PEM-Format
	 * @param AltNames   xsd:string[] Subject Alternative Names in der Form
	 *                   ("typ:wert", ...)
	 * @param AddName    xsd:string Vollständiger Name des Antragstellers
	 * @param AddEMail   xsd:string E-Mail Adresse des Antragstellers
	 * @param AddOrgUnit xsd:string Abteilung des Antragstellers
	 * 
	 * @return xsd:int Die Seriennummer des hochgeladenen Antrags
	 * @throws Exception
	 */
	public int createRequest(String PKCS10, String[] AltNames, String AddName, String AddEMail, String AddOrgUnit)
			throws Exception {

		return publicClient.newRequest(raId, PKCS10, AltNames, role, pin, AddName, AddEMail, AddOrgUnit,
				options.publish);
	}



	
}
