#!/bin/sh

docker tag localhost/branch-deployer ghcr.io/boudicca-events/branchdeployer

echo "pushing branchdeployer"
docker push ghcr.io/boudicca-events/branchdeployer
