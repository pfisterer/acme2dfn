package de.farberg.file2dfn.helpers;

import org.kohsuke.args4j.Option;

public class CommandLineOptions {

	@Option(name = "-configdir", usage = "Config dir with ra-id.txt, ca.p12, password.txt, ca-name.txt, role.txt, pin.txt")
	public String configDir = "/data/conf/";

	/**
	 * Path where the CSRs are picked up from and where certificates are written to
	 */
	@Option(name = "-csrPath")
	public String csrPath = "/data/csrs/";

	/**
	 * @param Publish xsd:boolean Veröffentlichung des Zertifikats
	 */
	@Option(name = "-publish", usage = "Veröffentlichung des Zertifikats")
	public boolean publish = true;

	@Option(name = "-dryrun", usage = "Dry run only")
	public boolean dryRun = false;

	public @Option(name = "-dryrunCsrFile") String dryRunCsrFile = null;

	public @Option(name = "-dryrunCertFile") String dryRunCertFile = null;

}