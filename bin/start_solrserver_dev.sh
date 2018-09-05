#!/bin/bash
set -x
# If this script is run by intellij, the docker must be detached since the run window isn't a tty. Therefore the default is -d.
# Console output can be seen with docker logs -f <container_ID>.
# If no version is specified, a new image will be build tagged as ${USER}
USER=${USER:-WHAT}    # silencing annoying intellij syntax quibble

package=solr
cid_file=solr.cid
docker_image="solrserver-updateservice"
version=${USER}
port=`id -u ${USER}`5
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
	# It's a bit dirty, but schema.xml is the only thing that are expected to change
	# so if solr-indexer/target and/or solr/target exist, they will not be rebuild (takes a small war)
	if [ ! -d target ]
	then
		mvn clean verify > /tmp/mvn.out.${USER}.solr-indexer
	fi
	echo ${pwd}
	rm -rf solr/docker/rawrepo-solr-indexer-solr-config-zip
	unzip target/rawrepo-solr-indexer-updateservice-1.10-SNAPSHOT-solr-config.zip -d solr/docker/rawrepo-solr-indexer-solr-config-zip
    cd solr/docker
    docker build -t ${docker_image}:${version} .
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
    if [ -f ${HOME}/.ocb-tools/SOLR_HOST_UPDATE ]
    then
        rm ${HOME}/.ocb-tools/SOLR_HOST_UPDATE
    fi
    if [ -f ${HOME}/.ocb-tools/SOLR_PORT_UPDATE ]
    then
        rm ${HOME}/.ocb-tools/SOLR_PORT_UPDATE
    fi
fi

echo "Starting container"
container_id=`docker run -it ${detached} -p ${port}:8983 \
		-e SOLR_AUTOSOFTCOMMIT_MAXTIME=100 \
		${docker_image}:${version}`
cc=$?
if [ ${cc} -ne 0 ]
then
    echo "Couldn't start"
    exit 1
else
    echo ${port} > ${HOME}/.ocb-tools/SOLR_PORT_UPDATE
    uname -n > ${HOME}/.ocb-tools/SOLR_HOST_UPDATE
    echo ${container_id} > ${HOME}/.ocb-tools/${cid_file}
    echo "PORT: ${port}"
    echo "CID : ${container_id}"
    imageName=`docker inspect --format='{{(index .Name)}}' ${container_id} | cut -d"/" -f2`
    echo "NAME: ${imageName}"
fi

