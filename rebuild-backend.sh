#!/bin/bash

DIRECTORY=/dynamodb-titan-storage-backend-master

rm -r ${DIRECTORY}/target
rm -rf /tmp/searchindex
rm -rf elasticsearch
rm -r ${DIRECTORY}/server

cd /${DIRECTORY}

sudo src/test/resources/install-gremlin-server.sh

sudo mvn install -X