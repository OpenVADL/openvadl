#!/bin/bash

cd /rtl

mkdir -p build

# Preserve sbt exit code when piping to tee
set -o pipefail

make test 2>&1 | tee build/test.log
