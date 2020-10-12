package de.farberg.file2dfn;

import org.kohsuke.args4j.Option;

public class CommandLineOptions {

	/**
	 * File containing the RA ID
	 * 
	 * @param RaID xsd:int Nummer der RA, 0 für die Master-RA
	 */
	@Option(name = "-ra-id-file", usage = "RA Id")
	String raIdFile = "/data/conf/ra-id.txt";

	/**
	 * File containing the client certificate
	 */
	@Option(name = "-p12file")
	String p12File = "/data/conf/ca.p12";

	/**
	 * File containing the certificate password
	 */
	@Option(name = "-passwordFile")
	String passwordFile = "/data/conf/password.txt";

	/**
	 * File containing the name of the CA
	 */
	@Option(name = "-caNameFile")
	String caNameFile = "/data/conf/ca-name.txt";

	/**
	 * Path where the CSRs are picked up from and where certificates are written to
	 */
	@Option(name = "-csrPath")
	String csrPath = "/data/csrs/";

	/**
	 * @param Role xsd:string Die Rolle des beantragten Zertifikats
	 */
	@Option(name = "-roleFile", usage = "Die Rolle des beantragten Zertifikats")
	String roleFile = "/data/conf/role.txt";

	/**
	 * @param Pin xsd:string Sperrkennwort für das Zertifikat als SHA-1 Hash
	 */
	@Option(name = "-pinFile")
	String pinFile = "/data/conf/pin.txt";

	/**
	 * @param Publish xsd:boolean Veröffentlichung des Zertifikats
	 */
	@Option(name = "-publish", usage = "Veröffentlichung des Zertifikats")
	boolean publish = true;

}