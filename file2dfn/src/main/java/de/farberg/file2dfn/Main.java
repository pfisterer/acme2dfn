package de.farberg.file2dfn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
			
			log.info("Using SMTP audit");
			return new SmtpAudit(options.auditSmtpUsername, options.auditSmtpPassword, options.auditSmtpSmtpHost, options.auditSmtpSmtpPort,
					options.auditSmtpSender, options.auditSmtpRecipient, options.auditSmtpSubject, options.auditSmtpText);
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
