package de.farberg.file2dfn;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

public class Helper {
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

					sans.add(title + ": " + name.getName());
				}
			}
		}

		return sans;
	}

	public static Object toPEMObject(String pem) throws Exception {		
		PEMParser pemParser = new PEMParser(new StringReader(pem));
		return pemParser.readObject();
	}
	
	public static PKCS10CertificationRequest toCertificationRequest(String csr) throws Exception {
		return (PKCS10CertificationRequest) toPEMObject(csr);
	}
	
	public static X509CertificateHolder toCertificate(String cert) throws Exception {
		return (X509CertificateHolder) toPEMObject(cert);
	}

}
