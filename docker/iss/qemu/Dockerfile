FROM ghcr.io/openvadl/aarch64-toolchain@sha256:1989ceedbf2c4ffb0b274ace03e3211f88bf1c24f9da462e22a990ed1617ed5e as aarch64
FROM ghcr.io/openvadl/riscv-toolchain:medany-rv32i-rv32im-rv32impriv-rv64i-rv64im-rv64impriv as riscv

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
RUN ../configure --target-list=riscv64-softmmu,aarch64-softmmu
RUN make -j $(nproc)
RUN make install

FROM ubuntu:22.04 AS final
COPY --from=build /usr/local/bin/ /usr/local/bin
COPY --from=riscv /opt/riscv /opt/riscv
COPY --from=aarch64 /opt/aarch64 /opt/aarch64