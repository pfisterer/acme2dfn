package de.farberg.file2dfn.client;

public interface DfnClient {

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
	int createRequest(String PKCS10, String[] AltNames, String AddName, String AddEMail, String AddOrgUnit)
			throws Exception;

	boolean approveRequest(int serialNumber) throws Exception;

	String getCertificate(int serialNumber) throws Exception;

}