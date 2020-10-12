package de.farberg.file2dfn;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class DryRun {
	private final static Logger log = Logger.getLogger(DryRun.class.getName());

	public static void main(String[] args) throws Exception {
		
		// Parse command line options
		CommandLineOptions options = parseCommandLineOptions(new String[] {});

		// Create fake testing env
		File confDir = new File("../private/configdir/");
		File copyCsr = new File("../private/csr-base64.txt");
		
		Path tempDirWithPrefix = Files.createTempDirectory("ca-test-");
		options.csrPath = tempDirWithPrefix.toString();
		options.raIdFile = new File(confDir, "ra-id.txt").getCanonicalPath();
		options.p12File = new File(confDir, "ca.p12").getCanonicalPath();
		options.passwordFile = new File(confDir, "password.txt").getCanonicalPath();
		options.caNameFile = new File(confDir, "ca-name.txt").getCanonicalPath();
		options.roleFile = new File(confDir, "role.txt").getCanonicalPath();
		options.pinFile = new File(confDir, "pin.txt").getCanonicalPath();

		JSONObject obj = new JSONObject();
		obj.put("AddName", "Someone");
		obj.put("AddEMail", "mail@example.org");
		obj.put("AddOrgUnit", "some-org-unit");
		
		Helper.writeFile(new File(tempDirWithPrefix.toFile(), "new-test1234.csr"), Helper.readAsciiFile(copyCsr));
		Helper.writeFile(new File(tempDirWithPrefix.toFile(), "new-test1234.csr.json"), obj.toString());
		
		log.config("Command line options are: " + options);

		// Create DFN client
		DfnClient dfnClient = new DfnClient(options);

		// Create Reconciler
		Reconciler reconciler = new Reconciler(dfnClient, new File(options.csrPath));
		reconciler.run();
	}

	private static CommandLineOptions parseCommandLineOptions(String[] args) throws CmdLineException {
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser parser = new CmdLineParser(options);
		parser.parseArgument(args);
		return options;
	}

}
