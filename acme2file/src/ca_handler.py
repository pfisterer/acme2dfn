#!/usr/bin/python
# -*- coding: utf-8 -*-
""" skeleton for customized CA handler """
from __future__ import print_function
# pylint: disable=E0401
from acme.helper import load_config
import os

class CAhandler(object):
    """ EST CA  handler """

    def __init__(self, _debug=None, logger=None):
        self.logger = logger
        self.logger.debug('DfnCaHandler::__init__()')
        self.parameter = None

    def __enter__(self):
        """ Makes CAhandler a Context Manager """
        if not self.parameter:
            self._config_load()
        
        self.logger.debug('DfnCaHandler::__enter__()')
        print("XXXXXXXXXXXXXXXXXXXXXXXXXX __enter__")
        return self

    def __exit__(self, *args):
        """ close the connection at the end of the context """
        self.logger.debug('DfnCaHandler::__exit__()')
        print("XXXXXXXXXXXXXXXXXXXXXXXXXX __exit__")

    def _config_load(self):
        """" load config from file """
        self.logger.debug('DfnCaHandler::_config_load()')
        print("XXXXXXXXXXXXXXXXXXXXXXXXXX _config_load")

        config_dic = load_config(self.logger, 'CAhandler')
        if 'parameter' in config_dic['CAhandler']:
            self.parameter = config_dic['CAhandler']['parameter']

        self.logger.debug('DfnCaHandler_config_load() ended')

    def _stub_func(self, parameter):
        """" load config from file """
        print("XXXXXXXXXXXXXXXXXXXXXXXXXX _stub_func")
        self.logger.debug('DfnCaHandler_stub_func({0})'.format(parameter))

        self.logger.debug('DfnCaHandler_stub_func() ended')

    def enroll(self, csr):
        """ enroll certificate  """
        #self.logger.debug('DfnCaHandler::enroll()', csr)
        #print("XXXXXXXXXXXXXXXXXXXXXXXXXX enroll", csr)

        print("-----BEGIN CERTIFICATE REQUEST-----")
        print(csr)
        print("-----END CERTIFICATE REQUEST-----")

        f = open("/tmp/bla.csr","w")
        f.write("-----BEGIN CERTIFICATE REQUEST-----\n")
        f.write(csr)
        f.write("\n")
        f.write("-----END CERTIFICATE REQUEST-----\n")
        f.close()
        os.system("openssl req -in  /tmp/bla.csr -noout -text")

        cert_bundle = None
        error = None
        cert_raw = None
        poll_indentifier = None
        self._stub_func(csr)

        self.logger.debug('DfnCaHandler::enroll() ended')

        return(error, cert_bundle, cert_raw, poll_indentifier)

    def poll(self, cert_name, poll_identifier, _csr):
        """ poll status of pending CSR and download certificates """
        self.logger.debug('DfnCaHandler::poll()')
        print("XXXXXXXXXXXXXXXXXXXXXXXXXX poll", cert_name, poll_identifier, _csr)
        error = None
        cert_bundle = None
        cert_raw = None
        rejected = False
        self._stub_func(cert_name)

        self.logger.debug('DfnCaHandler:poll() ended')
        return(error, cert_bundle, cert_raw, poll_identifier, rejected)

    def revoke(self, _cert, _rev_reason, _rev_date):
        """ revoke certificate """
        self.logger.debug('DfnCaHandler:revoke()', _cert, _rev_reason, _rev_date)
        print("XXXXXXXXXXXXXXXXXXXXXXXXXX revoke",_cert, _rev_reason, _rev_date)

        code = 500
        message = 'urn:ietf:params:acme:error:serverInternal'
        detail = 'Revocation is not supported.'

        self.logger.debug('DfnCaHandler::revoke() ended')
        return(code, message, detail)

    def trigger(self, payload):
        """ process trigger message and return certificate """
        self.logger.debug('DfnCaHandler::trigger()')
        print("XXXXXXXXXXXXXXXXXXXXXXXXXX trigger", payload)

        error = None
        cert_bundle = None
        cert_raw = None
        self._stub_func(payload)

        self.logger.debug('DfnCaHandler::trigger() ended with error: {0}'.format(error))
        return (error, cert_bundle, cert_raw)
