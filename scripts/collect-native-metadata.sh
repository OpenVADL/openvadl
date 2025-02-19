#!/usr/bin/env bash

set -e

cd "$(dirname "$0")/../"

rm -rf vadl-cli/main/resources/META-INF/native-image/vadl

./gradlew -Pagent run --args="check ../sys/risc-v/rv64im.vadl"
./gradlew metadataCopy --task run

./gradlew -Pagent run --args="iss --dump ../sys/risc-v/rv64im.vadl"
./gradlew metadataCopy --task run

./gradlew -Pagent run --args="lcb --dump -p rv32im ../sys/risc-v/rv32im.vadl"
./gradlew metadataCopy --task run