package de.farberg.file2dfn;

import java.io.File;
import java.util.logging.Logger;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class Main {
	private final static Logger log = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) throws Exception {
		// Parse command line options
		CommandLineOptions options = parseCommandLineOptions(args);
		log.config("Command line options are: " + options);

		// Create DFN client 
		DfnClient dfnClient = new DfnClient(options);
		
		// Create Reconciler
		Reconciler reconciler = new Reconciler(dfnClient, new File(options.csrPath));
				
		while (true) {
			reconciler.run();
			Thread.sleep(1000);
		}
	}


	private static CommandLineOptions parseCommandLineOptions(String[] args) throws CmdLineException {
		CommandLineOptions options = new CommandLineOptions();
		CmdLineParser parser = new CmdLineParser(options);
		parser.parseArgument(args);
		return options;
	}

}
