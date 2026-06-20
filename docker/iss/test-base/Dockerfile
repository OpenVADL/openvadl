# The QEMU (ISS) test base image comes with the required architecture toolchains, QEMU source code and sccache

ARG MERGED_TOOLCHAIN_IMAGE=ghcr.io/openvadl/merged-toolchain:latest
FROM ${MERGED_TOOLCHAIN_IMAGE} AS toolchains

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

# download rust for cosim
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
ENV PATH=/root/.cargo/bin:$PATH

# install cargo-chef to prebuild cosim dependencies into the base image
RUN cargo install cargo-chef --locked

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

WORKDIR /work

FROM base AS cosim-chef-planner
COPY vadl-cosim /work/vadl-cosim
WORKDIR /work/vadl-cosim
RUN cargo chef prepare --recipe-path recipe.json

FROM base AS cosim-chef-deps
WORKDIR /work/vadl-cosim
COPY --from=cosim-chef-planner /work/vadl-cosim/recipe.json recipe.json

# The QEMU ISS Test Base image, which contains the QEMU source code, toolchains and sccache
RUN cargo chef cook --release --recipe-path recipe.json

FROM cosim-chef-deps AS iss-test-base
COPY --from=toolchains /opt/riscv /opt/riscv
COPY --from=toolchains /opt/ppc64 /opt/ppc64
COPY --from=toolchains /opt/aarch32-linux /opt/aarch32-linux
COPY --from=toolchains /opt/aarch32-elf /opt/aarch32-elf
COPY --from=toolchains /opt/aarch64-linux /opt/aarch64-linux
COPY --from=toolchains /opt/aarch64-elf /opt/aarch64-elf

# Add toolchains to PATH
ENV TOOLCHAIN_PATH=/opt/riscv/bin:\
/opt/ppc64/bin:\
/opt/aarch32-elf/bin:\
/opt/aarch32-linux/bin:\
/opt/aarch64-elf/bin:\
/opt/aarch64-linux/bin

ENV PATH=${TOOLCHAIN_PATH}:$PATH
