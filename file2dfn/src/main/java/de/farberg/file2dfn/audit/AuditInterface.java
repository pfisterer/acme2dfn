package de.farberg.file2dfn.audit;

public interface AuditInterface {

	public void log(String subject, String json) throws Exception;
}
