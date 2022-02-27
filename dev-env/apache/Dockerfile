FROM httpd:2.4

RUN mkdir -p /usr/local/apache2/conf/sites/ /usr/local/apache2/htdocs/login
RUN echo 'IncludeOptional /conf/sites/*.conf' | tee -a /usr/local/apache2/conf/httpd.conf

COPY resources/custom.conf /conf/sites/custom.conf
COPY resources/key /key
COPY resources/cert /cert





