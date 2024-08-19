#!/usr/bin/env bash
# Lets you run the Dockerfile manually

set -e

docker build -t OpenVADL/iss-qemu-test-image -f Dockerfile ../../
docker run --rm -it -v ./manual_test_source:/work OpenVADL/iss-qemu-test-image "$@"
docker image prune -f
