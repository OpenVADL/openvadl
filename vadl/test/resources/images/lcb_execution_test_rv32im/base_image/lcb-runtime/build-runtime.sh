#!/usr/bin/env bash
# Builds the C runtime for an LCB toolchain with the LCB toolchain itself, so that all
# objects use the custom ELF relocations of the generated backend:
#
#   - picolibc            -> libc.a (includes libm), headers
#   - compiler-rt         -> libclang_rt.builtins.a (soft-float incl. quad precision long double,
#                            64-bit integer helpers)
#
# Usage: build-runtime.sh <picolibc-src> <compiler-rt-builtins-src> <prefix> <cross-file> <machine-dir>
#
#   <cross-file>   Meson cross file describing the target. Its [binaries] c entry is a GCC-style
#                  compiler driver for the target (e.g. lcb-rv32im-cc), which is also used for
#                  compiler-rt; its ar entry is used to archive compiler-rt. Its cpu_family must
#                  be a name unknown to picolibc (e.g. 'rv32im').
#   <machine-dir>  picolibc machine directory for that cpu_family: a meson.build which sets
#                  src_machine, and the sources it lists (at least setjmp.S).

set -euo pipefail

PICOLIBC_SRC=$(realpath "$1")
BUILTINS_SRC=$(realpath "$2")
PREFIX=$3
CROSS_FILE=$(realpath "$4")
MACHINE_DIR=$(realpath "$5")
CPU_FAMILY=$(sed -n "s/^[[:space:]]*cpu_family[[:space:]]*=[[:space:]]*'\([^']*\)'.*/\1/p" \
  "$CROSS_FILE")

# Prints a [binaries] entry of the cross file as a shell command line, e.g.
#   c = ['python3', '/lcb-runtime/lcb-rv32im-cc']  ->  python3 /lcb-runtime/lcb-rv32im-cc
# (Arguments must not contain spaces.)
cross_binary() {
  local value
  value=$(sed -n "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*//p" "$CROSS_FILE" | head -n 1 \
    | tr -d "[],'")
  if [ -z "$value" ]; then
    echo "no '$1' entry in [binaries] of $CROSS_FILE" >&2
    exit 1
  fi
  echo "$value"
}

CC=$(cross_binary c)
AR=$(cross_binary ar)
WORK=$(mktemp -d /tmp/lcb-runtime.XXXXXX)
JOBS=$(nproc)

mkdir -p "$PREFIX/lib" "$PREFIX/include"

# Compile all given sources in parallel; fail if any of them fails.
# Usage: compile_all <outdir> <flags> <sources...>
compile_all() {
  local outdir=$1 flags=$2
  shift 2
  mkdir -p "$outdir"
  printf '%s\0' "$@" | xargs -0 -P "$JOBS" -n 1 bash -c \
    'src=$4; $1 $2 -c "$src" -o "$3/$(basename "${src%.c}").o"' _ "$CC" "$flags" "$outdir"
}

###############################################################################
# picolibc
###############################################################################
echo "==> Building picolibc"
cp -r "$PICOLIBC_SRC" "$WORK/picolibc"

# The assembly sources of picolibc's existing architectures use GNU assembler syntax which the
# generated assembler does not understand. The cross file therefore uses a custom cpu_family, and
# the machine directory (meson.build + setjmp.S) for this family is installed into picolibc here.
PICOLIBC_MACHINE="$WORK/picolibc/newlib/libc/machine/$CPU_FAMILY"
cp -r "$MACHINE_DIR" "$PICOLIBC_MACHINE"

meson setup "$WORK/picolibc-build" "$WORK/picolibc" \
  --cross-file "$CROSS_FILE" \
  -Dprefix="$PREFIX" -Dincludedir=include -Dlibdir=lib \
  -Dbuildtype=release \
  -Dmultilib=false -Dpicocrt=false -Dsemihost=false -Dtests=false \
  -Dthread-local-storage=false -Dsingle-thread=true \
  -Dspecsdir=none -Dfortify-source=none
ninja -C "$WORK/picolibc-build" install

###############################################################################
# compiler-rt builtins
###############################################################################
echo "==> Building compiler-rt builtins"
# Builtins which need features the target does not have (atomics, TLS, x87/bf16 types, an OS).
EXCLUDE='^(atomic.*|emutls|enable_execute_stack|eprintf|clear_cache|gcc_personality_v0|trampoline_setup|os_version_check|crtbegin|crtend|.*xf.*|.*xc3|.*bf.*)\.c$'
mapfile -t BUILTINS < <(ls "$BUILTINS_SRC"/*.c | while read -r f; do
  basename "$f" | grep -qE "$EXCLUDE" || echo "$f"; done)
# Builtins which compiler-rt only provides as target-specific assembly (e.g. __mulsi3).
BUILTINS+=("$(dirname "$(realpath "$0")")"/builtins/*.c)

compile_all "$WORK/builtins" \
  "-O3 -ffreestanding -fno-builtin -fforce-enable-int128 -isystem $PREFIX/include" \
  "${BUILTINS[@]}"
rm -f "$PREFIX/lib/libclang_rt.builtins.a"
"$AR" rcs "$PREFIX/lib/libclang_rt.builtins.a" "$WORK"/builtins/*.o

rm -rf "$WORK"
echo "==> LCB runtime installed into $PREFIX"


