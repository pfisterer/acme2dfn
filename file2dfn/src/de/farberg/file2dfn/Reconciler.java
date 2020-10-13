package de.farberg.file2dfn;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.json.JSONObject;

import de.farberg.file2dfn.client.DfnClient;
import de.farberg.file2dfn.helpers.Helper;

public class Reconciler {
	private final static Logger log = Helper.getLogger(Reconciler.class.getName());
	private DfnClient dfnClient;
	private File csrPath;

	public Reconciler(DfnClient dfnClient, File csrPath) {
		this.dfnClient = dfnClient;
		this.csrPath = csrPath;
	}

	public void run() throws InterruptedException {

		log.info("Reconcile loop starting");

		// Process new CSRs
		File[] newCsrs = Helper.getFiles(csrPath, "new-", ".csr");
		Arrays.stream(newCsrs).forEach(csr -> {
			try {
				log.info("Processing new csr @ " + csr);
				processNewCsr(csr);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error processing new csr @ " + csrPath + ": " + e.getMessage(), e);
			}
		});

		// Process pending CSRs
		File[] pendingCsrs = Helper.getFiles(csrPath, "pending-", ".csr");
		Arrays.stream(pendingCsrs).forEach(csr -> {
			try {
				log.info("Processing pending csr @ " + csr);
				processPendingCsr(csr);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error processing pending csr @ " + csrPath + ": " + e.getMessage(), e);
			}
		});

	}

	private void processNewCsr(File csr) throws Exception {
		// Read csr to string
		String pkcs10 = Helper.readAsciiFile(csr);
		log.info("Read CSR from file: \n" + pkcs10);

		PKCS10CertificationRequest certificationRequest = Helper.toCertificationRequest(pkcs10);
		log.info("Converted to PKCS10CertificationRequest: \n" + certificationRequest.toString());

		// Get SANs from CSR
		String[] altNames = Helper.extractSanFromCsr(certificationRequest).toArray(new String[] {});
		log.info("Extracted AltNames: " + Arrays.toString(altNames));

		// Read additional data from JSON file
		File jsonFile = new File(csr.getCanonicalPath() + ".json");
		JSONObject data = new JSONObject(Helper.readAsciiFile(jsonFile));
		log.info("Extracted Metadata from (" + jsonFile.getAbsolutePath() + "):" + data.toString());
		String addName = data.getString("AddName");
		String addEMail = data.getString("AddEMail");
		String addOrgUnit = data.getString("AddOrgUnit");

		// Send CSR to CA
		int serialNumber = dfnClient.createRequest(pkcs10, altNames, addName, addEMail, addOrgUnit);
		log.info("New request with serial #" + serialNumber);

		// Approve the request
		boolean approveRequest = dfnClient.approveRequest(serialNumber);
		log.info("Approval result for serial #" + serialNumber + ": " + approveRequest);

		// Rename file to indicate it is being processed
		jsonFile.delete();
		File newFileName = newToPending(csr, serialNumber);
		log.info("Renaming csr (" + csr.getName() + ") to " + newFileName.getName());
		csr.renameTo(newFileName);

		log.info("Done processing new request");
	}

	private void processPendingCsr(File csr) throws Exception {
		int serial = dfnSerialFromPendingFilename(csr);
		log.info("Processing pending request, file = " + csr);

		String certificatePEM = dfnClient.getCertificate(serial);

		if (certificatePEM != null) {
			log.info("DFN returned non-empty reply: \n" + certificatePEM);

			X509CertificateHolder certificate = Helper.toCertificate(certificatePEM);
			log.info("Parsed PEM to certificate: " + certificate.toString());

			// Delete CSR and store Certificate
			csr.delete();
			String acmeId = acmeIdFromPendingFilename(csr);
			File newFileName = new File(csr.getParent(), "cert-" + acmeId + ".crt");
			Helper.writeFile(newFileName, certificatePEM);

			log.info("Wrote certificate to file: " + newFileName);

		} else {
			log.info("DFN returned empty reply for serial #" + serial);
		}

	}

	private static int dfnSerialFromPendingFilename(File file) throws Exception {
		Pattern regex = Pattern.compile("^pending-dfn-([0-9]+)-.*.csr");
		Matcher matcher = regex.matcher(file.getName());

		if (matcher.matches() && matcher.groupCount() == 1) {
			return Integer.parseInt(matcher.group(1));
		}

		throw new Exception("Unable to parse serial number from filename " + file.getName());
	}

	private static String acmeIdFromPendingFilename(File file) throws Exception {
		Pattern regex = Pattern.compile("^pending-dfn-[0-9]+-(.*).csr");
		Matcher matcher = regex.matcher(file.getName());

		if (matcher.matches() && matcher.groupCount() == 1) {
			return matcher.group(1);
		}

		throw new Exception("Unable to parse ACME id from filename " + file.getName());
	}

	private static File newToPending(File csr, int serial) {
		String filename = csr.getName();
		String newName = filename.replaceAll("^new-", "pending-dfn-" + serial + "-");
		return new File(csr.getParentFile(), newName);
	}

}
