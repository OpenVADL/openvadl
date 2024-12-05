#!/usr/bin/env bash

 set -e -x

 if [ -z "${LLVM_SOURCE_PATH}" ]; then
     echo "LLVM_SOURCE_PATH is not set"
 fi

 for INPUT in $(find c -name "*.c")
 do
     BASE_NAME=$(basename $INPUT)
     $LLVM_SOURCE_PATH/build/bin/clang --target=$1 -c -S $INPUT -o assertions/$1/$BASE_NAME.s
 done