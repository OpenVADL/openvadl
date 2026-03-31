#!/usr/bin/env bash
set -e

root=$(realpath $(dirname "$0"))

V="$root/../vadl" # <---- Potentially adjust this for you setup
VADL="$V/obj/bin/vadl"

mkdir -p "$root/bd-vadl"

run_in_embench_docker() {
  if [ -d /opt/aarch64-cross ]; then
    # Inside docker already
    $@
  else
    sleep 1 # Sometimes files are somehow not written yet
    "$root/run_in_docker.sh" embench "$@"
  fi
}

( cd "$V/sys/aarch64"

  rm -rf obj/newcpu2-ume/CPU/iss
  # We remove the stop condition for the DTC since we utilise
  # VADL's user mode emulation in order to exit from the simulator
  sed -E 's/([[:space:]]*)(stop.*)/\1\/\/ \2/' src/newcpu2.vadl > src/newcpu2-ume.vadl
  $VADL   src/newcpu2-ume.vadl -o obj --dtc
  rm      src/newcpu2-ume.vadl
  run_in_embench_docker "make -C obj/newcpu2-ume/CPU/iss/dtc/ume obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS"
  cp -v   obj/newcpu2-ume/CPU/iss/dtc/ume/obj/rel/CPU "$root/bd-vadl/vadl-dtc-aarch64"
)

( cd "$V/sys/risc-v"

  rm -rf obj/rv32i_m-ume/CPU/iss/dtc
  # We remove the stop condition for the DTC since we utilise
  # VADL's user mode emulation in order to exit from the simulator
  sed -E 's/([[:space:]]*)(stop.*)/\1\/\/ \2/' src/rv32i_m.vadl > src/rv32i_m-ume.vadl
  $VADL   src/rv32i_m-ume.vadl -o obj --dtc
  rm      src/rv32i_m-ume.vadl
  run_in_embench_docker "make -C obj/rv32i_m-ume/CPU/iss/dtc/ume obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS"
  cp -v   obj/rv32i_m-ume/CPU/iss/dtc/ume/obj/rel/CPU "$root/bd-vadl/vadl-dtc-rv32i_m"

  rm -rf  obj/rv32i_m_p1/CPU/cas
  $VADL   src/rv32i_m_p1.vadl -o obj --cas
  run_in_embench_docker "make -C obj/rv32i_m_p1/CPU/cas obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS"
  cp -v   obj/rv32i_m_p1/CPU/cas/obj/rel/CPU "$root/bd-vadl/vadl-cas-rv32i_m_p1"

  rm -rf  obj/rv32i_m_p2/CPU/cas
  $VADL   src/rv32i_m_p2.vadl -o obj --cas
  run_in_embench_docker "make -C obj/rv32i_m_p2/CPU/cas obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS"
  cp -v   obj/rv32i_m_p2/CPU/cas/obj/rel/CPU "$root/bd-vadl/vadl-cas-rv32i_m_p2"

  rm -rf  obj/rv32i_m_p3/CPU/cas
  $VADL   src/rv32i_m_p3.vadl -o obj --cas
  run_in_embench_docker "make -C obj/rv32i_m_p3/CPU/cas obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS"
  cp -v   obj/rv32i_m_p3/CPU/cas/obj/rel/CPU "$root/bd-vadl/vadl-cas-rv32i_m_p3"

  rm -rf  obj/rv32i_m_p5/CPU/cas
  $VADL   src/rv32i_m_p5.vadl -o obj --cas
  run_in_embench_docker "make -C obj/rv32i_m_p5/CPU/cas obj/rel/CPU FLAGS=-DUNCHECKED_MEMORY_ACCESS"
  cp -v   obj/rv32i_m_p5/CPU/cas/obj/rel/CPU "$root/bd-vadl/vadl-cas-rv32i_m_p5"

  rm -rf  obj/rv32i_m_p3/CPU/hdl
  $VADL   src/rv32i_m_p3.vadl -o obj --hdl
	# Run Scala part on host (arch independent) and compile to binary inside Docker
  make -C obj/rv32i_m_p3/CPU/hdl obj/CPU.v
  run_in_embench_docker "make -C obj/rv32i_m_p3/CPU/hdl obj/rel/CPU"
  cp -v   obj/rv32i_m_p3/CPU/hdl/obj/rel/CPU "$root/bd-vadl/vadl-hdl-rv32i_m_p3"
 
#   rm -rf  obj/rv32i_m_p5/CPU/hdl
#   $VADL   src/rv32i_m_p5.vadl -o obj --hdl
#   # Run Scala part on host (arch independent) and compile to binary inside Docker
#   make -C obj/rv32i_m_p5/CPU/hdl obj/CPU.v
#   run_in_embench_docker "make -C obj/rv32i_m_p5/CPU/hdl obj/rel/CPU"
#   cp -v   obj/rv32i_m_p5/CPU/hdl/obj/rel/CPU "$root/bd-vadl/vadl-hdl-rv32i_m_p5"
)
