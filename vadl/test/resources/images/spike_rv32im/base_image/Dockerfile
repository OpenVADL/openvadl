FROM ghcr.io/openvadl/riscv-toolchain@sha256:e919dcb27ebc3396f8ae40095adb960c09e54160cd0d2eb4481b50b05ac0c098 AS riscv-toolchain

# Spike

FROM debian:12 AS builder-spike-toolchain
ARG num_jobs=16
RUN apt update -y
RUN apt install -y build-essential autoconf automake autotools-dev python3 python3-pip libmpc-dev libmpfr-dev libgmp-dev gawk bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev ninja-build git cmake libglib2.0-dev curl device-tree-compiler
WORKDIR /root
RUN git clone https://github.com/riscv-software-src/riscv-isa-sim
WORKDIR riscv-isa-sim
RUN ./configure --prefix=/opt/spike && make -j$num_jobs && make install && make clean

# Rust

FROM rust:latest AS builder-fail-if
RUN cargo install --git https://github.com/kper/fail_if fail_if

# LLVM
FROM ghcr.io/openvadl/llvm19-base@sha256:ca1cd04e8c00e5e6c992696e6a0cb23ec0f104b76dc63e3b7b31d0d5699e58ad
RUN apt update -y && apt-get update --fix-missing
RUN apt-get install -y device-tree-compiler python3 python3-pip python3-pandas vim
COPY --from=riscv-toolchain /opt/riscv /opt/riscv
COPY --from=builder-spike-toolchain /opt/spike /opt/spike
COPY --from=builder-fail-if /usr/local/cargo/bin/fail_if /usr/bin/fail_if
WORKDIR /work
COPY helper /helper
COPY lcb_wrapper.sh .
COPY spike.sh .
COPY llvm.sh .
COPY filecheck.sh .
