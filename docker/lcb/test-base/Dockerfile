# The LCB test base image comes with the required architecture toolchains, QEMU and sccache installed

FROM ghcr.io/openvadl/merged-toolchain@sha256:346078ff2c7aea66a52cf563f6b09f0f02ec9e1f96ed52fec20d17077f1a3792 AS toolchains

FROM ubuntu:22.04 AS build

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

WORKDIR /qemu/build
RUN ../configure --prefix=/opt/qemu --target-list=riscv32-softmmu,riscv64-softmmu,aarch64-softmmu
RUN make -j $(nproc)
RUN make install

# The LCB Test Base image, which contains the QEMU binaries, toolchains and sccache
FROM ubuntu:22.04 AS lcb-test-base

COPY --from=build /opt/sccache /opt/sccache
COPY --from=build /opt/qemu /opt/qemu
COPY --from=toolchains /opt/riscv /opt/riscv
COPY --from=toolchains /opt/aarch64 /opt/aarch64

# Add toolchains to PATH
ENV PATH=/opt/riscv/bin:/opt/aarch64/bin:/opt/qemu/bin:/opt/sccache:$PATH
