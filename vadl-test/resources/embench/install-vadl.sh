#!/usr/bin/env bash

# This is taken from the sys/risc-v/Makefile targtet .attic/bin/vadl
# It is used to install vadl in a docker container.

set -e

GITEA_URL=https://${ACCESS_TOKEN}:@ea.complang.tuwien.ac.at
GITEA_API=${GITEA_URL}/api/v1
GITEA_REPO=${GITEA_API}/repos/vadl/vadl

cd $(realpath $(dirname "$0"))
mkdir -p vadl

curl -X GET "${GITEA_REPO}/releases" | yq > vadl/releases
yq '.[] | select(.name == "nightly").assets[] | select(.name == "vadl.zip").uuid' vadl/releases > vadl/release_uuid
echo "-- Installation VADL: ${GITEA_URL}/attachments/`cat vadl/release_uuid | tr -d '\"'`"
curl -L ${GITEA_URL}/attachments/`cat vadl/release_uuid | tr -d '\"'` -o vadl/vadl.zip
(cd vadl; yes | unzip vadl.zip)
cat vadl/release
vadl/bin/vadl -v
