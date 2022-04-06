#!/bin/bash
set -x
# If this script is run by intellij, the docker must be detached since the run window isn't a tty. Therefore the default is -d.
# Console output can be seen with docker logs -f <container_ID>.
# If no version is specified, a new image will be build tagged as ${USER}
USER=${USER:-WHAT}    # silencing annoying intellij syntax quibble
SOLR_HOST_UPDATE=${SOLR_HOST_UPDATE:-WHAT}
SOLR_PORT_UPDATE=${SOLR_PORT_UPDATE:-WHAT}

package=solr-indexer
cid_file=solr-indexer.cid
docker_image=docker-metascrum.artifacts.dbccloud.dk/rawrepo-solr-indexer
version=${USER}
port=`id -u ${USER}`3
detached="-d"
while getopts "p:v:u" opt; do
    case "$opt" in
    "u" )
            detached=""
            ;;
    "p" )
            port=$OPTARG
            ;;
    "v" )
            version=$OPTARG
            ;;
    esac
done

if [ ! -d ${HOME}/.ocb-tools ]
then
    mkdir ${HOME}/.ocb-tools
fi

if [ "$version" = "${USER}" ]
then
    echo "Building ${package}"
	mvn clean package > /tmp/mvn.out.${USER}.${package}
	echo "Done building"
    docker build -t ${docker_image}:${USER} .
    cc=$?
    if [ ${cc} -ne 0 ]
    then
        echo "Couldn't build image"
        exit 1
    fi
fi

if [ -f ${HOME}/.ocb-tools/${cid_file} ]
then
    docker stop `cat ${HOME}/.ocb-tools/${cid_file}`
fi

if [ -f ${HOME}/.ocb-tools/SOLR_HOST_UPDATE ]
then
    SOLR_HOST_UPDATE=`cat ${HOME}/.ocb-tools/SOLR_HOST_UPDATE`
fi
if [ -f ${HOME}/.ocb-tools/SOLR_PORT_UPDATE ]
then
    SOLR_PORT_UPDATE=`cat ${HOME}/.ocb-tools/SOLR_PORT_UPDATE`
fi

rr_conn=`egrep rawrepo.jdbc.conn.url ${HOME}/.ocb-tools/testrun.properties | tr -d " " | cut -d"/" -f3-`
rr_user=`egrep rawrepo.jdbc.conn.user ${HOME}/.ocb-tools/testrun.properties | tr -d " " | cut -d"=" -f2`
rr_pass=`egrep rawrepo.jdbc.conn.passwd ${HOME}/.ocb-tools/testrun.properties | tr -d " " | cut -d"=" -f2`
record_url=`grep recordservice.url ${HOME}/.ocb-tools/testrun.properties | cut -d"=" -f2 | tr -d " "`
echo "Starting container"
container_id=`docker run -it ${detached} -p ${port}:8080 \
		-e RAWREPO_URL="${rr_user} ${rr_user}:${rr_pass}@${rr_conn}" \
		-e MAX_CONCURRENT=1 \
		-e TIMEOUT=10 \
		-e SOLR_URL=http://${SOLR_HOST_UPDATE}:${SOLR_PORT_UPDATE}/solr/rawrepo\
		-e WORKER=socl-sync \
		-e RAWREPO_RECORD_URL="${record_url}" \
		${docker_image}:${version}`
cc=$?
if [ ${cc} -ne 0 ]
then
    echo "Couldn't start"
    exit 1
else
    echo ${container_id} > ${HOME}/.ocb-tools/${cid_file}
    echo "PORT: ${port}"
    echo "CID : ${container_id}"
    imageName=`docker inspect --format='{{(index .Name)}}' ${container_id} | cut -d"/" -f2`
    echo "NAME: ${imageName}"
fi

