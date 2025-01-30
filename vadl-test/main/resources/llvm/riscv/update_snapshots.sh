#!/bin/bash    

set -x
set -e

CURR_DIR=`pwd`
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
WORK_DIR=`mktemp -d -p "$DIR"`
VADL_BIN=$1
SPEC_NAME=$2
SPEC=$3

if [ -z ${VADL_BIN} ]; then echo "You have to set the path to the VADL binary"; exit 1; fi
if [ -z ${SPEC} ]; then echo "You have to set the path to the specification"; exit 1; fi
if [ -z ${SPEC_NAME} ]; then echo "You have to set the name of the folder in the assertions folder"; exit 1; fi

cp Dockerfile $WORK_DIR
mkdir $WORK_DIR/inputs
cp c/* $WORK_DIR/inputs
cp run.sh $WORK_DIR
cp update_snapshots.sh $WORK_DIR

cd $WORK_DIR
$VADL_BIN --viam-lcb $SPEC 

docker build -t update_assertions --build-arg TARGET=$SPEC_NAME .
docker run -e TARGET=$SPEC_NAME --rm -v $WORK_DIR/outputs:/output -t update_assertions

cd $CURR_DIR # Go back to the original folder
cp $WORK_DIR/outputs/* assertions/$SPEC_NAME
