#!/bin/sh

docker tag docker.io/library/branch-deployer:0.0.1-SNAPSHOT ghcr.io/boudicca-events/branchdeployer

echo "pushing branchdeployer"
docker push ghcr.io/boudicca-events/branchdeployer
