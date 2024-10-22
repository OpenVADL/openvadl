# RISCV64 toolchain
FROM jozott/riscv-toolchain

ARG QEMU_VERSION="9.0.3"
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
    python3-pip

# download qemu into the current directory
RUN curl -L $QEMU_RELEASE | tar -xvz --strip-components=1

# build qemu for riscv64
RUN mkdir build && cd build && \
    ../configure --enable-debug --target-list=riscv64-softmmu && \
    make -j 8

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