#!/bin/bash

set -e
set -x

CLANG=/src/llvm-final/build/bin/clang
DIS=/src/llvm-final/build/bin/llvm-dis
LLC=/src/llvm-final/build/bin/llc
FILECHECK=/src/llvm-final/build/bin/FileCheck

INPUT=/src/inputs/$INPUT
RUN=`grep '^; RUN:' "$INPUT" | sed 's/^; RUN: //'`
CMD="$CLANG $DIS $LLC $FILECHECK $INPUT $RUN"