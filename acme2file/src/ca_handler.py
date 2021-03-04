#!/usr/bin/python
# -*- coding: utf-8 -*-
""" skeleton for customized CA handler """
from __future__ import print_function
# pylint: disable=E0401
from acme.helper import load_config
import hashlib
import os
import base64


def banner(logger, fn):
    logger.info(f"vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv")
    logger.info(f"  DFN PKI CA Handler @ {fn} ")


def trailer(logger, fn):
    logger.info(f"{fn}________________________")


class CAhandler(object):
    """ DFN PKI CA handler """

    def __init__(self, _debug=None, logger=None):
        self.logger = logger
        banner(self.logger, "__init__")

        # Set default parameters
        self.shared_data_path = None

        trailer(self.logger, "__init__")

    def __enter__(self):
        """ Makes CAhandler a Context Manager """
        banner(self.logger, "__enter__")

        # Load config
        if not self.shared_data_path:
            self._config_load()

        trailer(self.logger, "__enter__")
        return self

    def __exit__(self, *args):
        """ close the connection at the end of the context """
        banner(self.logger, "__exit__")
        trailer(self.logger, "__exit__")

    def _config_load(self):
        """" load config from file """
        banner(self.logger, "_config_load")

        config_dic = load_config(self.logger, 'CAhandler')
        if 'shared_data_path' in config_dic['CAhandler']:
            self.shared_data_path = config_dic['CAhandler']['shared_data_path']
            self.logger.info(f"Using '{self.shared_data_path}' as shared data path")

        trailer(self.logger, "_config_load")

    def csr_to_id(self, csr):
        return hashlib.sha224(csr.encode()).hexdigest()

    def csr_to_file(self, csr, path):
        f = open(path, "w")
        f.write("-----BEGIN CERTIFICATE REQUEST-----\n")
        f.write(csr)
        f.write("\n")
        f.write("-----END CERTIFICATE REQUEST-----\n")
        f.close()

    def dump_csr(self, path):
        os.system(f"openssl req -in '{path}' -noout -text")

    def enroll(self, csr):
        """ enroll certificate  """
        banner(self.logger, "enroll")

        poll_id = self.csr_to_id(csr)
        path = f"{self.shared_data_path}/new-{poll_id}.csr"

        self.csr_to_file(csr, path)
        self.dump_csr(path)

        cert_bundle = None
        error = None
        cert_raw = None

        trailer(self.logger, "enroll")
        return(error, cert_bundle, cert_raw, poll_id)

    def poll(self, cert_name, poll_identifier, _csr):
        """ poll status of pending CSR and download certificates """
        banner(self.logger, "poll")

        self.logger.info(f"cert_name={cert_name}, poll_identifier={poll_identifier}, _csr={_csr}")

        # error - error message during cert polling (None in case no error occured)
        # cert_bundle - certificate chain in pem format
        # cert_raw - certificate in asn1 (binary) format - base64 encoded
        # poll_identifier - (updated) callback identifier - will be updated in database for later lookups
        # rejected - indicates of request has been rejected by CA admistrator - in case of a request rejection the corresponding order status will be set to "invalid" state

        error = None
        cert_bundle = None
        cert_raw = None
        rejected = False

        (cert_raw, cert_bundle) = self._internal_get_cert_by_poll_id(poll_identifier)

        trailer(self.logger, "poll")
        return(error, cert_bundle, cert_raw, poll_identifier, rejected)

    def revoke(self, _cert, _rev_reason, _rev_date):
        banner(self.logger, "revoke")
        """ revoke certificate """
        self.logger.info(f'DfnCaHandler:revoke(), _cert={_cert}, _rev_reason={_rev_reason}, _rev_date={_rev_date}')

        code = 500
        message = 'urn:ietf:params:acme:error:serverInternal'
        detail = 'Revocation is not supported.'

        trailer(self.logger, "revoke")
        return(code, message, detail)

    # cf. https://github.com/grindsa/acme2certifier/blob/master/docs/trigger.md
    def trigger(self, payload):
        """ process trigger message and return certificate """
        banner(self.logger, "trigger")

        print(f"trigger, payload={payload}")
        poll_identifier = base64.b64decode(payload).decode('ascii')

        (cert_raw, cert_bundle) = self._internal_get_cert_by_poll_id(poll_identifier)
        error = None

        trailer(self.logger, "trigger")
        return (error, cert_bundle, cert_raw)

    def _internal_get_cert_by_poll_id(self, poll_identifier):
        expected_cert_path = f"{self.shared_data_path}/cert-{poll_identifier}.crt"

        if os.path.isfile(expected_cert_path):
            self.logger.info(f"Found certificate for poll id {poll_identifier} @ {expected_cert_path}")

            file = open(expected_cert_path)
            cert_pem = file.read()
            file.close()

            cert_raw = cert_pem.replace('-----BEGIN CERTIFICATE-----\n',
                                        '').replace('-----END CERTIFICATE-----\n', '').replace('\n', '')
            cert_bundle = cert_pem

            return (cert_raw, cert_bundle)
