FROM docker-dbc.artifacts.dbccloud.dk/payara5-micro:latest

ENV KAFKA_LOG_APPNAME rawrepo-solr-indexer

COPY target/*.war deployments
COPY target/docker/config.json deployments
ENV LOGBACK_FILE file:///data/logback-include-stdout.xml

LABEL RAWREPO_URL="Raw repo jdbc url. (required). Ex.: 'user:pass@host:1234/dbname'" \
      SOLR_URL="(required)" \
      MAX_CONCURRENT="Default is 2 (optional)" \
      WORKER="Default is solr-sync (optional)" \
      TIMEOUT="Default is 5 (optional)" \
      RAWREPO_RECORD_URL="The URL to the rawrepo record service endpoint (required)."
