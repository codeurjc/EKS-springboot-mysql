#!/bin/bash -x
set -eu -o pipefail

docker build -t ${REGISTRY}/${IMAGE_NAME}:${TAG} .
