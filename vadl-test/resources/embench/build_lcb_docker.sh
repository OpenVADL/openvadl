#!/usr/bin/env bash
set -e

root=$(realpath $(dirname "$0"))

VADL="$root/vadl/bin/vadl"

( cd /vadl-src
  git fetch
  git rebase origin/master
)

$VADL --lcb /vadl-src/sys/risc-v/src/rv32i_m.vadl -o obj
cp -r obj/rv32i_m/CPU/lcb/llvm/llvm /llvm
cp -r obj/rv32i_m/CPU/lcb/llvm/clang /llvm
cp -r obj/rv32i_m/CPU/lcb/llvm/lld /llvm

export CCACHE_DIR=/mnt/tmp/ccache

( cd /llvm/build
  cmake \
    -DLLVM_ENABLE_PROJECTS="clang;lld" \
    -DLLVM_PARALLEL_COMPILE_JOBS=16 \
    -DLLVM_PARALLEL_LINK_JOBS=2 \
    -DLLVM_TARGETS_TO_BUILD="RISCV" \
    -DLLVM_EXPERIMENTAL_TARGETS_TO_BUILD="CPU" \
    -DCMAKE_BUILD_TYPE=MinSizeRel \
    -DLLVM_ENABLE_ASSERTIONS=ON \
    -DLLVM_BUILD_TOOLS=Off \
    -DLLVM_CCACHE_PATH=/mnt/tmp/ccache \
    -DLLVM_CCACHE_SIZE=10.0G \
    -DLLVM_CCACHE_BUILD=On \
    -G \
    Ninja \
    ../llvm

  cmake \
    --build . \
    --target llc \
    --target opt \
    --target clang \
    --target lld \
    --target llvm-mc \
    --target llvm-objdump
)

