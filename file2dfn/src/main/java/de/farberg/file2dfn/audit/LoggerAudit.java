package de.farberg.file2dfn.audit;

import java.util.logging.Logger;

import de.farberg.file2dfn.Main;
import de.farberg.file2dfn.helpers.Helper;

public class LoggerAudit implements AuditInterface{
	private final static Logger log = Helper.getLogger(Main.class.getName());
	
	@Override
	public void log(String json) throws Exception {
		log.info("Audit log: "+json);
	}

}
