#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

./build_all.py --cc aarch64-none-elf-gcc --arch aarch64 --chip generic --board virt-iss --clean  "$@"
