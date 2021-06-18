package de.farberg.file2dfn;

import static de.farberg.file2dfn.helpers.Helper.readFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.json.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import de.farberg.file2dfn.audit.AuditInterface;
import de.farberg.file2dfn.audit.LoggerAudit;
import de.farberg.file2dfn.audit.SmtpAudit;
import de.farberg.file2dfn.client.DfnClient;
import de.farberg.file2dfn.client.DfnClientDummy;
import de.farberg.file2dfn.client.DfnClientSoap;
import de.farberg.file2dfn.helpers.CommandLineOptions;
import de.farberg.file2dfn.helpers.Helper;

public class Main {
	private final static Logger log = Helper.getLogger(Main.class.getName());

	public static void main(String[] args) throws Exception {
		// Parse command line options
		CommandLineOptions options = parseCommandLineOptions(args);
		log.config("Command line options: " + options);

		// Create DFN client
		AuditInterface audit = getAudit(options);
		
		if(options.auditTest) {
			JSONObject json = new JSONObject();
			json.put("time", ZonedDateTime.now( ZoneOffset.UTC ).format( DateTimeFormatter.ISO_INSTANT ));
			json.put("action", "auditTest");
			json.put("message", "This message has been sent to test the audit functionality");
			
			audit.log("auditTest",json.toString(3));
			System.exit(0);
		}
		
		DfnClient dfnClient = getClient(audit, options);
		
		// Create Reconciler
		Reconciler reconciler = new Reconciler(dfnClient, audit, options);

		// Run reconcile loop
		do {
			reconciler.run();
			Thread.sleep(5000);
		} while (true && !options.dryRun);
	}

	private static DfnClient getClient(AuditInterface audit, CommandLineOptions options) throws Exception {
		if (options.dryRun) {
			prepareDryRun(options);
		}

		if (options.dryRun || "dummy".equals(options.client)) {
			log.info("Using dummy client");
			return new DfnClientDummy(audit, options);
		} else {
			log.info("Using DFN Soap client");
			return new DfnClientSoap(audit, options);
		}
	}

	private static AuditInterface getAudit(CommandLineOptions options) throws Exception {
		
		if ("smtp".equals(options.audit)) {
			String smtpConfigFile = new File(options.configDir, "audit-smtp.json").getCanonicalPath();
			String privateKeyFileName= new File(options.configDir, "audit-smtp-priv.pem").getCanonicalPath();
			String publicKeyFileName = new File(options.configDir, "audit-smtp-pub.pem").getCanonicalPath();

			log.info("Using SMTP audit, reading JSON config from UTF-8 file @ " + smtpConfigFile);
			JSONObject json = new JSONObject(readFile(smtpConfigFile, StandardCharsets.UTF_8));

			log.info("SMTP audit, reading key (file: "+privateKeyFileName+")and cert ("+publicKeyFileName+") as ASCII files");
			
			String publKey = Helper.readAsciiFile(publicKeyFileName);
			AsymmetricKeyParameter privKey = Helper.loadPrivateKey(new FileInputStream(privateKeyFileName));
			
			return new SmtpAudit(
							json.getString("Username"), 
							json.getString("Password"), 
							json.getString("Host"), 
							json.getInt("Port"),
							json.getString("Sender"), 
							json.getString("Recipient"), 
							json.getString("Subject"), 
							json.getString("Text"),
							publKey,
							privKey
						);
		}

		log.info("Using logging audit");
		return new LoggerAudit();
	}

	private static void prepareDryRun(CommandLineOptions options) throws IOException {
		Path tempDirWithPrefix = Files.createTempDirectory("ca-test-");
		options.csrPath = tempDirWithPrefix.toFile().getPath();

		File copyCsrFile = new File(options.dryRunCsrFile);
		Helper.writeFile(new File(options.csrPath, "new-acme123.csr"), Helper.readAsciiFile(copyCsrFile));

		JSONObject obj = new JSONObject();
		obj.put("AddName", "Someone");
		obj.put("AddEMail", "mail@example.org");
		obj.put("AddOrgUnit", "some-org-unit");
		Helper.writeFile(new File(options.csrPath, "new-acme123.csr.json"), obj.toString());
	}

	private static CommandLineOptions parseCommandLineOptions(String[] args) throws CmdLineException {
		log.config("Parsing command line arguments: " + Arrays.stream(args).map(el -> ("'" + el + "'")).collect(Collectors.toList()));
		CommandLineOptions options = new CommandLineOptions();
		if (args.length > 0) {
			CmdLineParser parser = new CmdLineParser(options);
			parser.parseArgument(args);

			if (options.help) {
				parser.printUsage(System.out);
				System.exit(0);
			}

		}
		return options;
	}

}
