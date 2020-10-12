# ACME (aka Letsencrypt) support for DFN's PKI

**This is work in progress, most things do not work fully work, syet**

---

This project provides support for ACME-based certificates using the [DFN PKI](https://www.pki.dfn.de/ueberblick-dfn-pki/). The DFN PKI provides a [SOAP-based API](https://blog.pki.dfn.de/2019/04/soap-client-version-3-8-4-0-1/) for requesting and approving certificate requests. However, it currently does not support ACME clients.

Hence, this project implements the following: `ACME client <---> ACME server <---> DFN PKI SOAP API`.

## Overview 

It is based on the ACME server implementation [acme2certifier](https://github.com/grindsa/acme2certifier) (in Python) and the SOAP client provided by DFN-CERT Services GmbH, Hamburg, Germany and its contributors. (cf. [SOAP-Client Version 3.8.1/4.0.2](https://blog.pki.dfn.de/2019/11/soap-client-version-3-8-1-4-0-2/)) written in Java. 

This project is comprised of two parts:
- **acme2file**: A custom [ca_handler.py](https://github.com/grindsa/acme2certifier/blob/master/docs/ca_handler.md) for acme2certifier.
- **file2dfn**: A Java program interacting with DFN's SOAP service.

### acme2file

A Python handler script that receives incoming CSRs from acme2certifier (the ACME server). 

It works as follows:
- Invoked by acme2certifier, it receives incoming CSRs from ACME clients (after the [challenges](https://letsencrypt.org/docs/challenge-types/) have completed)
- Writes CSRs to a directory
- Waits for certificates to appear in this directory 
- Notifies acme2certifier

### file2dfn

Java program for interacting with DFN's SOAP API to request certificates and to approve them.

It works as follows:
- It watches for new CSRs in the aforementioned directory (stored there by acme2file).
- Submits each CSR to the DFN SOAP server and approves them
- Polls the SOAP server for matching certificates and stores them in the directory

# Using 

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

and populate the files according to [DFN PKI's](https://www.pki.dfn.de/ueberblick-dfn-pki/) documentation. 

Then, create config map (add `-o yaml --dry-run` to see the result only) in your Kubernetes cluster:

```bash
kubectl create configmap acme2file-configmap --from-file=private/configdir 
```

## Run the ACME server

Run `skaffold dev`

# Testing 

## Run an interactive ACME test client

Do this once:

```bash
# Create an interactive pod with certbot installed
kubectl delete pod/certbot ; kubectl run certbot --rm -ti --image certbot/certbot -- /bin/bash

# Obtain the primary IP and the pods Kubernetes DNS name
# cf. https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/#a-aaaa-records-1
PRIMARY_IP=`ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1'`
MY_HOSTNAME="`echo $PRIMARY_IP | tr '.' '-'`.default.pod.cluster.local"
CERTBOT_COMMON_ARGS="--config-dir /tmp/acme/conf --work-dir /tmp/acme/work --logs-dir /tmp/acme/logs --agree-tos -m bla@bla.de --server http://acme2file-service --no-eff-email --standalone"

# Register the account with the ACME server  
certbot register "$CERTBOT_COMMON_ARGS"
```

Run repeatedly to test:

```bash
# Obtain a certificate using a standalone local server
certbot certonly "$CERTBOT_COMMON_ARGS" --preferred-challenges http -d "$MY_HOSTNAME" --cert-name certbot-test
```

## Acknowledgement

Libraries used and included in this repository
- This product includes software developed by DFN-CERT Services GmbH, Hamburg, Germany and its contributors. (cf. [SOAP-Client Version 3.8.1/4.0.2](https://blog.pki.dfn.de/2019/11/soap-client-version-3-8-1-4-0-2/))
- [kohsuke/args4j](https://github.com/kohsuke/args4j)
- [Bouncycastle](https://www.bouncycastle.org/)
