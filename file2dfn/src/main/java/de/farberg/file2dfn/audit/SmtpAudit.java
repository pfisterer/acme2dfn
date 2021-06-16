package de.farberg.file2dfn.audit;

import java.util.Properties;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

public class SmtpAudit implements AuditInterface {
	private Properties mailProps = new Properties();
	private Authenticator authenticator;
	private String recipient;
	private String sender;
	private String subject;
	private String text;

	public SmtpAudit(String username, String password, String smtpHost, int smtpPort, String sender, String recipient, String subject, String text) {
		mailProps.put("mail.smtp.auth", "true");
		mailProps.put("mail.smtp.starttls.enable", "true");
		mailProps.put("mail.smtp.host", smtpHost);
		mailProps.put("mail.smtp.port", smtpPort);

		this.authenticator = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		};

		this.recipient = recipient;
		this.sender = sender;
		this.subject = subject;
		this.text = text;
	}

	@Override
	public void log(String json) throws Exception {
		// Setup
		Session session = Session.getInstance(mailProps, authenticator);
		Message message = new MimeMessage(session);

		// Sender and recipient
		message.setFrom(new InternetAddress(sender));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
		message.setSubject(subject);
		message.setText(text);

		// Multipart message with attachment
		Multipart multipart = new MimeMultipart();
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		DataSource source = new ByteArrayDataSource(json, "application/json");
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName("audit.json");
		multipart.addBodyPart(messageBodyPart);
		message.setContent(multipart);

		// Send
		Transport.send(message);
	}

}
