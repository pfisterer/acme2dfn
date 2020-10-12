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

# Open Issues

Currently, DFN imposes strict requirements on different fields in CSRs that they generate certificates for. This includes restrictions on `cn` (must not be empty) and `dn`. 

This is an issue since ACME clients create the CSR (and sign it) and thus no data can be added to them by this proxy implementation.

Here is how the created CSRs look like when dumped to a file by `acme2file`. 

In Docker, running

```bash
certbot certonly --config-dir /tmp/acme/conf --work-dir /tmp/acme/work
--logs-dir /tmp/acme/logs --server http://localhost:22280/  -m
bla@example.com --no-eff-email --agree-tos --standalone
--preferred-challenges http -d host.docker.internal --cert-name
certbot-test
```

yields the following CSR output:

```console
-----BEGIN CERTIFICATE REQUEST-----
MIICdzCCAV8CAQIwADCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL47YHbQ7YBOJGAnsvHa0LxFNeQvdC303ZHpazRtGGP6148sAKHku22klNxKSCGqCYLUINIEVdJxM5eZAX+stGnq0CBAd78jSMBWAoOCR0YYwOHbaKwRKxWBhVW8F1TdjQ9Z8IfYWdmNS/t08ZGPZU86AT3XvtZoq91yiaGBiMwMuotcAJGhpdQmWQ5kSoQsVagQNiVp6tcHZeyjjU2RXx4FskxkEZqQGd7ka50RN1rsOh1/+VHkKB6mm1WgqO4bpegpU28z5WyHj7QHy45iM6247LnuKnFdUzzumt9P2wJ4vppk/A5N9i1j860/fdthKNDPgocjBAzo/VNLF9N1qocCAwEAAaAyMDAGCSqGSIb3DQEJDjEjMCEwHwYDVR0RBBgwFoIUaG9zdC5kb2NrZXIuaW50ZXJuYWwwDQYJKoZIhvcNAQELBQADggEBAGAkAwQeH8nxpJiQYKr4BYl9BKH+14SxMWSnHTFrSKb8J6JhreP14pVGRmhkvaz3GmngsXqJK5pjh4MMZV9LDLVQWv7bNcRL6HcEYPFd9SsHtmNnABP8qlr5VEHC1I2PHjPxG92/iZ+ejQA/t7ZpqYSPlmiR8hOwVz68inZEVjg/7VEtQPXrsmbOlCCsDuCJsvtwB6LmfYH9uLpRy2W8sb2Jr87T//U8HYz6+Yo6qVB+LRjiyicSyakNaar4zd9zfpTJ3gIcfxtdKNq3nQi4r7QBQUGqMDfdngVmTfloDsB82xjpSuykXm0/6YdnKMz+Gjy7Do1ZXQe+Go7BuopEpos=
-----END CERTIFICATE REQUEST-----
```

A human readable version can be obtained by running `openssl req -in  demo.csr -noout -text`:

```console
Certificate Request:
    Data:
        Version: 3 (0x2)
        Subject:
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                RSA Public-Key: (2048 bit)
                Modulus:
                    00:be:3b:60:76:d0:ed:80:4e:24:60:27:b2:f1:da:
                    d0:bc:45:35:e4:2f:74:2d:f4:dd:91:e9:6b:34:6d:
                    18:63:fa:d7:8f:2c:00:a1:e4:bb:6d:a4:94:dc:4a:
                    48:21:aa:09:82:d4:20:d2:04:55:d2:71:33:97:99:
                    01:7f:ac:b4:69:ea:d0:20:40:77:bf:23:48:c0:56:
                    02:83:82:47:46:18:c0:e1:db:68:ac:11:2b:15:81:
                    85:55:bc:17:54:dd:8d:0f:59:f0:87:d8:59:d9:8d:
                    4b:fb:74:f1:91:8f:65:4f:3a:01:3d:d7:be:d6:68:
                    ab:dd:72:89:a1:81:88:cc:0c:ba:8b:5c:00:91:a1:
                    a5:d4:26:59:0e:64:4a:84:2c:55:a8:10:36:25:69:
                    ea:d7:07:65:ec:a3:8d:4d:91:5f:1e:05:b2:4c:64:
                    11:9a:90:19:de:e4:6b:9d:11:37:5a:ec:3a:1d:7f:
                    f9:51:e4:28:1e:a6:9b:55:a0:a8:ee:1b:a5:e8:29:
                    53:6f:33:e5:6c:87:8f:b4:07:cb:8e:62:33:ad:b8:
                    ec:b9:ee:2a:71:5d:53:3c:ee:9a:df:4f:db:02:78:
                    be:9a:64:fc:0e:4d:f6:2d:63:f3:ad:3f:7d:db:61:
                    28:d0:cf:82:87:23:04:0c:e8:fd:53:4b:17:d3:75:
                    aa:87
                Exponent: 65537 (0x10001)
        Attributes:
        Requested Extensions:
            X509v3 Subject Alternative Name:
                DNS:host.docker.internal
    Signature Algorithm: sha256WithRSAEncryption
         60:24:03:04:1e:1f:c9:f1:a4:98:90:60:aa:f8:05:89:7d:04:
         a1:fe:d7:84:b1:31:64:a7:1d:31:6b:48:a6:fc:27:a2:61:ad:
         e3:f5:e2:95:46:46:68:64:bd:ac:f7:1a:69:e0:b1:7a:89:2b:
         9a:63:87:83:0c:65:5f:4b:0c:b5:50:5a:fe:db:35:c4:4b:e8:
         77:04:60:f1:5d:f5:2b:07:b6:63:67:00:13:fc:aa:5a:f9:54:
         41:c2:d4:8d:8f:1e:33:f1:1b:dd:bf:89:9f:9e:8d:00:3f:b7:
         b6:69:a9:84:8f:96:68:91:f2:13:b0:57:3e:bc:8a:76:44:56:
         38:3f:ed:51:2d:40:f5:eb:b2:66:ce:94:20:ac:0e:e0:89:b2:
         fb:70:07:a2:e6:7d:81:fd:b8:ba:51:cb:65:bc:b1:bd:89:af:
         ce:d3:ff:f5:3c:1d:8c:fa:f9:8a:3a:a9:50:7e:2d:18:e2:ca:
         27:12:c9:a9:0d:69:aa:f8:cd:df:73:7e:94:c9:de:02:1c:7f:
         1b:5d:28:da:b7:9d:08:b8:af:b4:01:41:41:aa:30:37:dd:9e:
         05:66:4d:f9:68:0e:c0:7c:db:18:e9:4a:ec:a4:5e:6d:3f:e9:
         87:67:28:cc:fe:1a:3c:bb:0e:8d:59:5d:07:be:1a:8e:c1:ba:
         8a:44:a6:8b
```

This means that neither `cn` nor `dn` fields are set. Only a single `Subject Alternative Name` (SAN) entry is set.

**This requires that DFN PKI must set these fields accordingly when creating the certificate. Until this is the case, this project is not developed any further.**

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
