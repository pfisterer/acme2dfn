package de.farberg.file2dfn.client;

import java.io.File;

import de.farberg.file2dfn.audit.AuditInterface;
import de.farberg.file2dfn.helpers.CommandLineOptions;
import de.farberg.file2dfn.helpers.Helper;

public class DfnClientDummy implements DfnClient {

	private static int serialNo = 1;

	private CommandLineOptions options;


	public DfnClientDummy(AuditInterface audit, CommandLineOptions options) {
		this.options = options;
	}

	@Override
	public int createRequest(String PKCS10, String[] AltNames, String AddName, String AddEMail, String AddOrgUnit, String subject)
			throws Exception {

		return serialNo++;
	}

	@Override
	public boolean approveRequest(int serialNumber) throws Exception {

		File copyCert = new File(options.dryRunCertFile);
		Helper.writeFile(new File(options.csrPath, "cert-12345.crt"), Helper.readAsciiFile(copyCert));

		return true;
	}

	@Override
	public String getCertificate(int serialNumber) throws Exception {
		return Helper.readAsciiFile(options.dryRunCertFile);
	}

}
