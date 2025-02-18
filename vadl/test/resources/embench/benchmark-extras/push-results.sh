#!/usr/bin/env bash

set -e

cd $(realpath $(dirname "$0"))

TAG=$(git rev-parse HEAD)
LOCALREPO=results-repo

REPO=https://${ACCESS_TOKEN}@ea.complang.tuwien.ac.at/vadl/embench-results

if [ ! -d $LOCALREPO ]
then
  git clone $REPO $LOCALREPO
  cd $LOCALREPO
else
  cd $LOCALREPO
  git pull $REPO
fi

mkdir $TAG
cp -r ../results $TAG
cp -r ../results-aarch64-dtc $TAG
cp -r ../results-rv32-cas $TAG
cp -r ../results-rv32-dtc $TAG
git add $TAG
git commit -m "Add results of embench $TAG"
git push
