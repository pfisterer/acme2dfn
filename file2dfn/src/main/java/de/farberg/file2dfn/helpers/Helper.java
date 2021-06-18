package de.farberg.file2dfn.helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

public class Helper {
	private static Logger log = getLogger(Helper.class.getName());

	public static String readAsciiFile(String path) throws IOException {
		return Files.readString(Paths.get(path), StandardCharsets.US_ASCII);
	}

	public static String readAsciiFile(File path) throws IOException {
		return readAsciiFile(path.getAbsolutePath());
	}

	public static String readFile(String path, Charset charset) throws IOException {
		return Files.readString(Paths.get(path), charset);
	}

	public static void writeFile(File file, String contents) throws IOException {
		FileWriter fileWriter = new FileWriter(file, false);
		fileWriter.write(contents);
		fileWriter.flush();
		fileWriter.close();
	}

	public static File[] getFiles(File dir, String prefix, String suffix) {

		File[] matchingFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix) && name.endsWith(suffix);
			}
		});

		return matchingFiles;
	}

	public static List<String> extractSanFromCsr(PKCS10CertificationRequest csr) {
		List<String> sans = new ArrayList<>();
		Attribute[] certAttributes = csr.getAttributes();

		for (Attribute attribute : certAttributes) {

			if (attribute.getAttrType().equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
				Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
				GeneralNames gns = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
				GeneralName[] names = gns.getNames();

				for (GeneralName name : names) {
					String title = "";
					if (name.getTagNo() == GeneralName.dNSName) {
						title = "DNS";
					} else if (name.getTagNo() == GeneralName.iPAddress) {
						title = "IP Address";
					} else if (name.getTagNo() == GeneralName.otherName) {
						title = "Other Name";
					}

					sans.add(title + ":" + name.getName());
				}
			}
		}

		return sans;
	}

	public static AsymmetricKeyParameter loadPublicKey(InputStream is) {
		SubjectPublicKeyInfo spki = (SubjectPublicKeyInfo) readPemObject(is);
		try {
			return PublicKeyFactory.createKey(spki);
		} catch (IOException ex) {
			throw new RuntimeException("Cannot create public key object based on input data", ex);
		}
	}

	public static AsymmetricKeyParameter loadPrivateKey(InputStream is) {
		PEMKeyPair keyPair = (PEMKeyPair) readPemObject(is);
		PrivateKeyInfo pki = keyPair.getPrivateKeyInfo();
		try {
			return PrivateKeyFactory.createKey(pki);
		} catch (IOException ex) {
			throw new RuntimeException("Cannot create private key object based on input data", ex);
		}
	}

	private static Object readPemObject(InputStream is) {
		try {
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			PEMParser pemParser = new PEMParser(isr);

			Object obj = pemParser.readObject();
			if (obj == null) {
				throw new Exception("No PEM object found");
			}
			return obj;
		} catch (Throwable ex) {
			throw new RuntimeException("Cannot read PEM object from input data", ex);
		}
	}

	public static Object toPEMObject(String pem) throws Exception {
		PEMParser pemParser = new PEMParser(new StringReader(pem));
		return pemParser.readObject();
	}

	public static PKCS10CertificationRequest toCertificationRequest(String csr) throws Exception {
		return (PKCS10CertificationRequest) toPEMObject(csr);
	}

	public static X509CertificateHolder toCertificate(String cert) throws Exception {
		Object pemObject = toPEMObject(cert);
		log.info("Parsing PEM:\n" + cert);
		log.info("Object is: " + pemObject);
		return (X509CertificateHolder) pemObject;
	}

	public static Logger getLogger(String name) {
		Logger logger = Logger.getLogger(name);
		return logger;
	}

}
