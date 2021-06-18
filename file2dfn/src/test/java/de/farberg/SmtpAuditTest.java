package de.farberg;

import static de.farberg.file2dfn.helpers.Helper.readFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.json.JSONObject;

import de.farberg.file2dfn.Main;
import de.farberg.file2dfn.audit.SmtpAudit;
import de.farberg.file2dfn.helpers.Helper;

public class SmtpAuditTest {
	private final static Logger log = Helper.getLogger(Main.class.getName());

	public static void main(String[] args) throws Exception {
		SmtpAudit smtpAudit;

		// Setup
		{
			String configDir = "../private/configdir";
			String smtpConfigFile = new File(configDir, "audit-smtp.json").getCanonicalPath();
			String privateKeyFileName = new File(configDir, "audit-smtp-priv.pem").getCanonicalPath();
			String publicKeyFileName = new File(configDir, "audit-smtp-pub.pem").getCanonicalPath();

			log.info("Using SMTP audit, reading JSON config from UTF-8 file @ " + smtpConfigFile);
			JSONObject json = new JSONObject(readFile(smtpConfigFile, StandardCharsets.UTF_8));

			log.info("SMTP audit, reading key (file: " + privateKeyFileName + ")and cert (" + publicKeyFileName + ") as ASCII files");

			String publKey = Helper.readAsciiFile(publicKeyFileName);
			AsymmetricKeyParameter privKey = Helper.loadPrivateKey(new FileInputStream(privateKeyFileName));

			smtpAudit = new SmtpAudit(json.getString("Username"), json.getString("Password"), json.getString("Host"), json.getInt("Port"),
					json.getString("Sender"), json.getString("Recipient"), json.getString("Subject"), json.getString("Text"), publKey, privKey);
		}

		// Audit
		{
			JSONObject auditJson = new JSONObject();
			auditJson.put("time", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
			auditJson.put("action", "newRequest");
			auditJson.put("subject", "Test");
			smtpAudit.log("auditTest", auditJson.toString());
		}

	}
}
