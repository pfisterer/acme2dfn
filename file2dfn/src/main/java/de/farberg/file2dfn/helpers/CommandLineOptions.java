package de.farberg.file2dfn.helpers;

import org.kohsuke.args4j.Option;

public class CommandLineOptions {
	@Option(name = "-help")
	public boolean help = false;

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

	@Option(name = "-dryrunCsrFile")
	public String dryRunCsrFile = null;

	@Option(name = "-dryrunCertFile")
	public String dryRunCertFile = null;

	@Option(name = "-addName")
	public String addName = null;

	@Option(name = "-addEMail")
	public String addEMail = null;

	@Option(name = "-addOrgUnit")
	public String addOrgUnit = null;

	@Option(name = "-client")
	public String client = "dfn";
	
	@Option(name = "-triggerUrl")
	public String triggerUrl = "http://127.0.0.1:80/trigger";

	@Option(name = "-dnPrefix")
	public String dnPrefix = null;

	// --------------------------------------------------------------------
	// Audit
	// --------------------------------------------------------------------

	@Option(name = "-audit", usage="Audit log type (one of 'logger', 'smtp')")
	public String audit = "logger";

	
	@Option(name = "-auditMailUsername", usage="SMTP audit: Username")
	public String auditSmtpUsername = "";
	
	@Option(name = "-auditMailPassword", usage="SMTP audit: Password")
	public String auditSmtpPassword = "";
	
	@Option(name = "-auditMailSmtpHost", usage="SMTP audit: SmtpHost")
	public String auditSmtpSmtpHost = "localhost";
	
	@Option(name = "-auditMailSmtpPort", usage="SMTP audit: SmtpPort")
	public int auditSmtpSmtpPort = 25;
	
	@Option(name = "-auditMailSender", usage="SMTP audit: Sender")
	public String auditSmtpSender = "";
	
	@Option(name = "-auditMailRecipient", usage="SMTP audit: Recipient")
	public String auditSmtpRecipient = "";
	
	@Option(name = "-auditMailSubject", usage="SMTP audit: Subject")
	public String auditSmtpSubject = "[file2dfn] Audit log message";
	
	@Option(name = "-auditMailText", usage="SMTP audit: Text")
	public String auditSmtpText = "See attachment for details";
	
}