FROM docker.dbc.dk/payara-micro:latest

ENV KAFKA_LOG_APPNAME rawrepo-solr-indexer

ADD docker/config.d/* config.d
ADD target/*.war wars

ENV LOGBACK_FILE file:///data/logback-include-stdout.xml

LABEL RAWREPO_URL="Raw repo jdbc url. (required). Ex.: 'user:pass@host:1234/dbname'" \
      OPENAGENCY_URL="URL to openagency (defaults to 'http://openagency.addi.dk/2.33/')" \
      SOLR_URL="(required)" \
      MAX_CONCURRENT="Default is 2 (optional)" \
      WORKER_NAME="Default is solr-sync (optional)" \
      TIMEOUT="Default is 5 (optional)" \
      RAWREPO_RECORD_URL="The URL to the rawrepo record service endpoint (required)."