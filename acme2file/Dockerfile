FROM grindsa/acme2certifier:nginx-wsgi

RUN pip3 install --upgrade --no-cache-dir pyopenssl

COPY src/ /var/www/acme2certifier/volume/

