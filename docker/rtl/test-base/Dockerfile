FROM ghcr.io/openvadl/merged-toolchain@sha256:2ceaf5369996a5f3db0e0bb8e16ae5fe53003d6ac180bd9e60a9ced954f99a9a AS toolchains
FROM ghcr.io/openvadl/firtool@sha256:84c7b84322068a9b800c908a13656a6d756a507751230dad1c99c38b96bad335 AS firtool

FROM ubuntu:22.04 AS build

SHELL ["/bin/bash", "-c"]

# Install some basic utilities
ENV DEBIAN_FRONTEND=noninteractive
RUN apt update &&  \
    apt install -y \
    curl \
    zip \
    unzip \
    git \
    autoconf \
    build-essential \
    python3 \
    opensta

# Install SDKMAN, GraalVM & SBT
RUN curl -s "https://get.sdkman.io" | bash
RUN source "$HOME/.sdkman/bin/sdkman-init.sh" && \
    sdk install java 25-graal && \
    sdk install sbt 1.11.7 && \
    sbt --version # Init sbt runner

# Add GraalVM and SBT to PATH
ENV PATH=/root/.sdkman/candidates/sbt/current/bin:/root/.sdkman/candidates/java/current/bin:$PATH

# Install Scala-Cli
RUN curl -sSLf https://scala-cli.virtuslab.org/get | bash
RUN source ~/.profile

# Install OSS CAD Suite (contains yosys, verilator and other RTL synthesis tools)
ARG OSS_CAD_PATH=/opt/oss-cad-suite
ARG OSS_CAD_RELEASE=2025-10-04
ARG OSS_CAD_VERSION=20251004

ARG TARGETPLATFORM

RUN set -eux \
    && mkdir $OSS_CAD_PATH \
    && if [ "$TARGETPLATFORM" = "linux/amd64" ]; then ARCH=x64; fi \
    && if [ "$TARGETPLATFORM" = "linux/arm64" ]; then ARCH=arm64; fi \
    && OSS_CAD_PKG="https://github.com/YosysHQ/oss-cad-suite-build/releases/download/${OSS_CAD_RELEASE}/oss-cad-suite-linux-${ARCH}-${OSS_CAD_VERSION}.tgz" \
    && curl -sL ${OSS_CAD_PKG} | tar xz --strip-components=1 -C ${OSS_CAD_PATH}

# Add everything to OSS-CAD to PATH3
ENV PATH=${OSS_CAD_PATH}/bin:$PATH

# Import Risc-V Toolchain
COPY --from=toolchains /opt/riscv /opt/riscv

# Add toolchain to PATH
ENV PATH=/opt/riscv/bin:$PATH

# Import Firtool
COPY --from=firtool /opt/circt /opt/circt
ENV CHISEL_FIRTOOL_PATH=/opt/circt

# ####
# Check out & build RISC-V test suite at /riscv-tests
# ####

WORKDIR /
RUN git clone https://github.com/riscv/riscv-tests

WORKDIR /riscv-tests
RUN git checkout b5ba87097c42aa41c56657e0ae049c2996e8d8d8
RUN git submodule update --init --recursive

# Remove multicore disable because mhartid is not implemented
RUN sed -ie '/#define RISCV_MULTICORE_DISABLE/{n;N;d}' ./env/p/riscv_test.h
RUN sed -ie '/csrr a0, mhartid;/d' ./env/p/riscv_test.h

# Patch ecall to also write test result to tohost address
RUN sed -i $'/ecall/s//la a4, tohost; \\\\\\\nsw TESTNUM, 0(a4); \\\\\\\n&/' ./env/p/riscv_test.h

# Build test suite
RUN autoconf
RUN ./configure --prefix=/usr
ENV RISCV_GCC_OPTS="-static -mcmodel=medany -fvisibility=hidden -nostdlib -nostartfiles"
RUN make isa

# ####
# Done building RISC-V test suite
# ####

RUN mkdir /rtl
WORKDIR /rtl
