#!/usr/bin/env bash
set -e

root=$(realpath $(dirname "$0"))

VADL="$root/vadl/bin/vadl"

mkdir -p "$root/bd-vadl"

BUILD_TIME_RESULTS="$root/benchmark-extras/results/sim-build-times"
mkdir -p $BUILD_TIME_RESULTS

( cd /vadl-src
  git fetch
  git rebase origin/master
)

( cd "/vadl-src/sys/aarch64"
  rm -rf obj

  # We remove the stop condition for the DTC since we utilise
  # VADL's user mode emulation in order to exit from the simulator
  sed -E 's/([[:space:]]*)(stop.*)/\1\/\/ \2/' src/newcpu2.vadl > src/newcpu2-ume.vadl
  $VADL   src/newcpu2-ume.vadl -o obj --dtc
  rm      src/newcpu2-ume.vadl

  make -C obj/newcpu2-ume/CPU/iss/dtc/ume obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS
  cp -v   obj/newcpu2-ume/CPU/iss/dtc/ume/obj/rel/CPU "$root/bd-vadl/vadl-dtc-aarch64"
)

( cd "/vadl-src/sys/risc-v"
  rm -rf obj

  # We remove the stop condition for the DTC since we utilise
  # VADL's user mode emulation in order to exit from the simulator
  sed -E 's/([[:space:]]*)(stop.*)/\1\/\/ \2/' src/rv32i_m.vadl > src/rv32i_m-ume.vadl
  time (
  $VADL   src/rv32i_m-ume.vadl -o obj --dtc
  rm      src/rv32i_m-ume.vadl
  make -C obj/rv32i_m-ume/CPU/iss/dtc/ume obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS
  ) 2> $BUILD_TIME_RESULTS/vadl-dtc-rv32i_m
  cp -v   obj/rv32i_m-ume/CPU/iss/dtc/ume/obj/rel/CPU "$root/bd-vadl/vadl-dtc-rv32i_m"

  time (
  $VADL   src/rv32i_m_p1.vadl -o obj --cas
  make -C obj/rv32i_m_p1/CPU/cas obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS
  ) 2> $BUILD_TIME_RESULTS/vadl-cas-rv32i_m_p1
  cp -v   obj/rv32i_m_p1/CPU/cas/obj/rel/CPU "$root/bd-vadl/vadl-cas-rv32i_m_p1"

  time (
  $VADL   src/rv32i_m_p2.vadl -o obj --cas
  make -C obj/rv32i_m_p2/CPU/cas obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS
  ) 2> $BUILD_TIME_RESULTS/vadl-cas-rv32i_m_p2
  cp -v   obj/rv32i_m_p2/CPU/cas/obj/rel/CPU "$root/bd-vadl/vadl-cas-rv32i_m_p2"

  time (
  $VADL   src/rv32i_m_p3.vadl -o obj --cas
  make -C obj/rv32i_m_p3/CPU/cas obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS
  ) 2> $BUILD_TIME_RESULTS/vadl-cas-rv32i_m_p3
  cp -v   obj/rv32i_m_p3/CPU/cas/obj/rel/CPU "$root/bd-vadl/vadl-cas-rv32i_m_p3"

  time (
  $VADL   src/rv32i_m_p5.vadl -o obj --cas
  make -C obj/rv32i_m_p5/CPU/cas obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS
  ) 2> $BUILD_TIME_RESULTS/vadl-cas-rv32i_m_p5
  cp -v   obj/rv32i_m_p5/CPU/cas/obj/rel/CPU "$root/bd-vadl/vadl-cas-rv32i_m_p5"

  time (
  $VADL   src/rv32i_m_p3.vadl -o obj --hdl
  make -C obj/rv32i_m_p3/CPU/hdl obj/rel/CPU
  ) 2> $BUILD_TIME_RESULTS/vadl-hdl-rv32i_m_p3
  cp -v   obj/rv32i_m_p3/CPU/hdl/obj/rel/CPU "$root/bd-vadl/vadl-hdl-rv32i_m_p3"

  # $VADL   src/rv32i_m_p5.vadl -o obj --hdl
  # make -C obj/rv32i_m_p5/CPU/hdl obj/rel/CPU
  # cp -v   obj/rv32i_m_p5/CPU/hdl/obj/rel/CPU "$root/bd-vadl/vadl-hdl-rv32i_m_p5"
)
