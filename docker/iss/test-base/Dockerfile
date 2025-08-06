# The QEMU (ISS) test base image comes with the required architecture toolchains, QEMU source code and sccache

FROM ghcr.io/openvadl/merged-toolchain@sha256:2ceaf5369996a5f3db0e0bb8e16ae5fe53003d6ac180bd9e60a9ced954f99a9a AS toolchains

FROM ubuntu:22.04 AS base

ARG QEMU_VERSION="9.2.2"
ENV QEMU_RELEASE="https://github.com/qemu/qemu/archive/refs/tags/v$QEMU_VERSION.tar.gz"
ENV QEMU_DIR=/qemu
ENV PATH=$QEMU_DIR/build:$PATH

WORKDIR /qemu

RUN apt update &&  \
    apt install -y \
    curl \
    git \
    build-essential  \
    libglib2.0-dev  \
    libfdt-dev  \
    libpixman-1-dev  \
    zlib1g-dev  \
    ninja-build  \
    python3  \
    python3-venv  \
    python3-pip \
    netcat

# required by the test framework
RUN pip install pyyaml qemu.qmp tomli

# download qemu into the current directory
RUN curl -L $QEMU_RELEASE | tar -xvz --strip-components=1

# just make build dir
RUN mkdir build

WORKDIR /work

# install sccache for cloud build cache
ENV PATH=/opt/sccache:$PATH
RUN mkdir /opt/sccache
RUN set -ex; \
    ARCH=$(uname -m); \
    case "$ARCH" in \
        x86_64) SUFFIX="x86_64-unknown-linux-musl" ;; \
        aarch64) SUFFIX="aarch64-unknown-linux-musl" ;; \
        *) echo "Unsupported architecture: $ARCH" >&2; exit 1 ;; \
    esac; \
    curl -L https://github.com/mozilla/sccache/releases/download/v0.8.2/sccache-v0.8.2-$SUFFIX.tar.gz \
    | tar -xz -C /opt/sccache --strip-components=1

# The QEMU ISS Test Base image, which contains the QEMU source code, toolchains and sccache
FROM base AS iss-test-base
COPY --from=toolchains /opt/riscv /opt/riscv
COPY --from=toolchains /opt/aarch64-linux /opt/aarch64-linux
COPY --from=toolchains /opt/aarch64-elf /opt/aarch64-elf

# Add toolchains to PATH
ENV PATH=/opt/riscv/bin:/opt/aarch64-elf/bin:/opt/aarch64-linux/bin:$PATH