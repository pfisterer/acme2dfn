# ACME (aka Letsencrypt) support for DFN's PKI

This project provides support for ACME-based certificates using the [DFN PKI](https://www.pki.dfn.de/ueberblick-dfn-pki/). The DFN PKI provides a [SOAP-based API](https://blog.pki.dfn.de/2019/04/soap-client-version-3-8-4-0-1/) for requesting and approving certificate requests. However, it currently does not support ACME clients.

Hence, this project implements the following: `ACME client <---> ACME server <---> DFN PKI SOAP API`.

## Overview 

It is based on the ACME server implementation [acme2certifier](https://github.com/grindsa/acme2certifier) (in Python) and the SOAP client provided by DFN-CERT Services GmbH, Hamburg, Germany and its contributors. (cf. [SOAP-Client Version 4.3](https://blog.pki.dfn.de/2021/02/soap-client-version-4-3/)) written in Java. 

This project is comprised of two parts:
- **acme2file**: A custom [ca_handler.py](https://github.com/grindsa/acme2certifier/blob/master/docs/ca_handler.md) for acme2certifier.
- **file2dfn**: A Java program interacting with DFN's SOAP service.

### acme2file

A Python handler script that receives incoming CSRs from acme2certifier (the ACME server). 

It works as follows:
- Invoked by acme2certifier, it receives incoming CSRs from ACME clients (after the [challenges](https://letsencrypt.org/docs/challenge-types/) have completed)
- Writes CSRs to a directory
- Waits for certificates to appear in this directory
- Notifies acme2certifier by invoking the [trigger endpoint](https://github.com/grindsa/acme2certifier/blob/master/docs/trigger.md)

### file2dfn

Java program for interacting with DFN's SOAP API to request certificates and to approve them.

It works as follows:
- It watches for new CSRs in the aforementioned directory (stored there by acme2file). The files must have a prefix `new-` and suffix `.csr`. Everything in between is treated as an identifier used by `acme2file`.
- Submits each CSR to the DFN SOAP server and approves them
- Polls the SOAP server for matching certificates and stores them in the directory in the form `"cert-" + acmeId + ".crt"`.

# Usage

## Configuration
Create a directory structure as follows:

```console
private/configdir/
├── ca-name.txt
├── ca.p12
├── password.txt
├── pin.txt
├── ra-id.txt
└── role.txt
```

and populate the files (except for `audit-smtp*`) according to [DFN PKI's](https://www.pki.dfn.de/ueberblick-dfn-pki/) documentation (e.g., role.txt may contain `Web Server`). 

### Using SMTP Audit Logging

To use SMTP for auditing purposes, configure command line option `-audit smtp`

Add the folloging files to the configmap abouve.

Set SMTP options in  `audit-smtp.json` as follows

```JSON
{
	"Username": "...",
	"Password": "...",
	"Host": "...",
	"Port": 25,
	"Sender": "...",
	"Recipient": "...",
	"Subject": "[acme2file] Audit log message",
	"Text": "See attachment."
}
```

and create a private (`audit-smtp-priv.pem`) and public (`audit-smtp-pub.pem `) key using commands such as:

```console 
openssl genrsa -out audit-smtp-priv.pem 4096
openssl rsa -in audit-smtp-key.pem -pubout -outform PEM -out audit-smtp-pub.pem 
```

To verify the signature in the mail, use a command similar to this:

```bash
cat audit.json | openssl dgst -sha512 -verify private/configdir/audit-smtp-pub.pem -signature signature.asc
```

### Create configuration map

Then, create config map  in your Kubernetes cluster:

```bash
kubectl create configmap acme2file-configmap --from-file=private/configdir 
```

(add `-o yaml --dry-run` to see the result only)

## Run the ACME server

Run `skaffold dev`

# Testing 

## file2dfn dry-run (i.e., without accessing the SOAP API)

For development, a local dry-run is supported:

```bash
(cd file2dfn ; mvn exec:java -Dexec.mainClass=de.farberg.file2dfn.Main -Dexec.args="-dryrun -configdir ../private/configdir/ -dryrunCsrFile ../private/csr-base64.txt -dryrunCertFile ../private/example-cert.pem")
```

## Testing with certbot

To run an interactive ACME test client in Kubernetes, use the following command:

```bash
# Create an interactive pod with certbot installed
kubectl delete pod/certbot ; kubectl run certbot --command=true --rm -ti --image certbot/certbot -- /bin/sh

# Obtain the primary IP and the pods Kubernetes DNS name
# cf. https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/#a-aaaa-records-1
PRIMARY_IP=`ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1'`
MY_HOSTNAME="`echo $PRIMARY_IP | tr '.' '-'`.default.pod.cluster.local"
CERTBOT_COMMON_ARGS="--config-dir /tmp/acme/conf --work-dir /tmp/acme/work --logs-dir /tmp/acme/logs --agree-tos -m bla@bla.de --server http://acme2file-service --no-eff-email --standalone --debug"

# Register the account with the ACME server and obtain certificate
rm -rf /tmp/acme ; certbot $CERTBOT_COMMON_ARGS register ; certbot $CERTBOT_COMMON_ARGS --preferred-challenges http -d "$MY_HOSTNAME" --cert-name certbot-test certonly
```

## Acknowledgements

Libraries used and included in this repository
- This product includes software developed by DFN-CERT Services GmbH, Hamburg, Germany and its contributors. (cf. [SOAP-Client Version 3.8.1/4.0.2](https://blog.pki.dfn.de/2019/11/soap-client-version-3-8-1-4-0-2/))
- [kohsuke/args4j](https://github.com/kohsuke/args4j)
- [Bouncycastle](https://www.bouncycastle.org/)

## Internal Stuff

### Record asciinema

Record asciinema in Iterm2
- `tmux -CC new -s myrec` and resize to 150 x 40
- `tmux set status off ; export PS1='${PWD/*\//}$ '`
- Split screen as required

In another shell
- Run `asciinema rec --overwrite -i 2 -c "tmux attach -t myrec" demo.cast`
- Detach the session in tmux: `ctrl + b + d`
- Convert to SVG using `cat demo.cast | svg-term --out demo.svg --term iterm2 --window --profile iterm2`

### Add a new version of DFN's libs

These are stored in the folder `file2dfn/local-maven-repo`

```bash
# Soapclient
mvn deploy:deploy-file \
	-Durl=file:./local-maven-repo/ \
	-DrepositoryId=local-maven-repo \
	-DupdateReleaseInfo=true \
	-DgroupId=de.dfncert \
	-DartifactId=soapclient \
	-Dversion=4.3 \
	-Dfile=soapclient-4.3.jar

# Utils
mvn deploy:deploy-file \
	-Durl=file:./local-maven-repo/ \
	-DrepositoryId=local-maven-repo \
	-DupdateReleaseInfo=true \
	-DgroupId=de.dfncert \
	-DartifactId=utils \
	-Dversion=2.13 \
	-Dfile=utils-2.13.jar
```
