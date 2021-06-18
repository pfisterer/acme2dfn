package de.farberg.file2dfn.audit;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.signers.RSADigestSigner;
import org.json.JSONObject;

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
	private AsymmetricKeyParameter privKey;

	public SmtpAudit(String username, String password, String smtpHost, int smtpPort, String sender, String recipient, String subject, String text,
			String publKey, AsymmetricKeyParameter privKey) throws Exception {
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

		this.privKey = privKey;

		JSONObject auditJson = new JSONObject();
		auditJson.put("time", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
		auditJson.put("action", "startup");
		auditJson.put("publickey", publKey);
		log("Startup - information about public key", auditJson.toString());
	}

	@Override
	public void log(String subject, String json) throws Exception {
		// Setup
		Session session = Session.getInstance(mailProps, authenticator);
		Message message = new MimeMessage(session);

		// Sender and recipient
		message.setFrom(new InternetAddress(sender));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
		message.setSubject(this.subject + " - " + subject);
		message.setText(text);

		// Multipart message with attachment
		Multipart multipart = new MimeMultipart();

		// JSON
		{
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			DataSource source = new ByteArrayDataSource(json, "application/json");
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName("audit.json");
			multipart.addBodyPart(messageBodyPart);
			message.setContent(multipart);
		}

		// Signature
		{
			RSADigestSigner signer = new RSADigestSigner(new SHA512Digest());
			signer.init(true, this.privKey);
			signer.update(json.getBytes(), 0, json.getBytes().length);
			byte[] signature = signer.generateSignature();

			MimeBodyPart messageBodyPart = new MimeBodyPart();
			DataSource source = new ByteArrayDataSource(signature, "application/pkcs7-signature");
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName("signature.asc");
			multipart.addBodyPart(messageBodyPart);
			message.setContent(multipart);
		}

		// Send
		Transport.send(message);
	}

}
