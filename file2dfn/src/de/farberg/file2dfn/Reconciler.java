package de.farberg.file2dfn;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.json.JSONObject;

import de.farberg.file2dfn.client.DfnClient;
import de.farberg.file2dfn.helpers.CommandLineOptions;
import de.farberg.file2dfn.helpers.Helper;

public class Reconciler {
	private final static Logger log = Helper.getLogger(Reconciler.class.getName());
	private final HttpClient httpClient = HttpClient.newBuilder().build();
	private DfnClient dfnClient;
	private File csrPath;
	private CommandLineOptions options;

	public Reconciler(DfnClient dfnClient, CommandLineOptions options) {
		this.dfnClient = dfnClient;
		this.csrPath = new File(options.csrPath);
		this.options = options;
	}

	public void run() throws InterruptedException {
		// log.info("Reconcile loop starting");

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

		// Set default data for the certificate request API
		String addName = options.addName;
		String addEMail = options.addEMail;
		String addOrgUnit = options.addOrgUnit;

		// Read additional data from JSON file (if it exists)
		File jsonFile = new File(csr.getCanonicalPath() + ".json");
		if (jsonFile.exists()) {
			JSONObject data = new JSONObject(Helper.readAsciiFile(jsonFile));
			log.info("Extracted Metadata from (" + jsonFile.getAbsolutePath() + "):" + data.toString());
			addName = data.getString("AddName");
			addEMail = data.getString("AddEMail");
			addOrgUnit = data.getString("AddOrgUnit");
		}

		// Get subject
		String subject = extractSubject(certificationRequest);

		// Create DN contents (cf. https://blog.pki.dfn.de/2021/02/umstellung-notwendig-aenderungen-am-soap-api/)
		String dn = (options.dnPrefix != null ? options.dnPrefix + "," : "")  + "CN=" + subject;
		
		// Send CSR to CA
		int serialNumber = dfnClient.createRequest(pkcs10, altNames, addName, addEMail, addOrgUnit, dn);
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

			log.info("Notifying acme2file @ trigger url " + options.triggerUrl);
			notifyAcme2File(acmeId);

		} else {
			log.info("DFN returned empty reply for serial #" + serial);
		}

	}

	private int notifyAcme2File(String acmeId) throws Exception {
		String base64Payload = Base64.getEncoder().encodeToString(acmeId.getBytes());
		String json = "{\"payload\":\"" + base64Payload + "\"}";

		HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(json)).uri(URI.create(options.triggerUrl))
				.header("Content-Type", "application/json").build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		
		log.info("Request for acmeID"+acmeId+" returned " + response.statusCode());

		return response.statusCode();
	}

	private String extractSubject(PKCS10CertificationRequest certificationRequest) {
		String subject = null;

		// Extract from CSR
		X500Name x500Name = certificationRequest.getSubject();
		if (x500Name != null) {
			String x500NameString = x500Name.toString().trim();
			if (x500NameString.length() > 0) {
				log.info("Extracted subject name from CSR: '" + x500NameString + "'");
				subject = x500NameString;
			}
		}

		// Generate CN from first AltName (e.g., "DNS: 172-17-0-5.default.pod.cluster.local")
		String[] altNames = Helper.extractSanFromCsr(certificationRequest).toArray(new String[] {});

		if (subject == null && altNames.length > 0) {
			String firstAltName = altNames[0];
			String prefix = "DNS:";

			if (firstAltName.startsWith(prefix)) {
				subject = firstAltName.substring(prefix.length()).trim();
				log.info("Extracted subject from 1st altname: '" + subject + "'");
			}
		}

		return subject;
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
