#!/usr/bin/env bash

image=$1
shift
cmd=$@

docker run --rm -v .:/embench $image sh -c "cd /embench; $cmd"
