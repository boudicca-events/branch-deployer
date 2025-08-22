#!/bin/sh

docker tag localhost/branchdeployer ghcr.io/boudicca-events/branchdeployer

echo "pushing branchdeployer"
docker push ghcr.io/boudicca-events/branchdeployer
