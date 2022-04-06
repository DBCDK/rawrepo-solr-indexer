#!/bin/bash
set -x
# If this script is run by intellij, the docker must be detached since the run window isn't a tty. Therefore the default is -d.
# Console output can be seen with docker logs -f <container_ID>.
# If no version is specified, a new image will be build tagged as ${USER}
USER=${USER:-WHAT}    # silencing annoying intellij syntax quibble

package=solr
cid_file=solr.cid
docker_image="docker-metascrum.artifacts.dbccloud.dk/rawrepo-solr-fbs-server"
docker_version="master-$(curl https://is.dbc.dk/job/rawrepo/job/solr-config-fbs/job/master/lastSuccessfulBuild/buildNumber)"
port=`id -u ${USER}`5
detached="-d"
while getopts "p:u" opt; do
    case "$opt" in
    "u" )
            detached=""
            ;;
    "p" )
            port=$OPTARG
            ;;
    esac
done

if [ ! -d ${HOME}/.ocb-tools ]
then
    mkdir ${HOME}/.ocb-tools
fi

echo "Using docker image ${docker_image}:${docker_version}"

docker pull ${docker_image}:${docker_version}

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
		${docker_image}:${docker_version}`
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

