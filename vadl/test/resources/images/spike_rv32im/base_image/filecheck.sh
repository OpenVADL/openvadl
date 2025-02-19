#!/bin/bash

set -e
set -x

export INPUT=/src/inputs/$INPUT
RUN=`grep '^; RUN:' "$INPUT" | sed 's/^; RUN: //'`

eval "$RUN"