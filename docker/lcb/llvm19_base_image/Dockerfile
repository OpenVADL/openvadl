FROM ubuntu
LABEL org.opencontainers.image.source=https://github.com/OpenVADL/open-vadl
RUN apt-get update -y
RUN apt-get install -y make cmake ninja-build build-essential ccache git curl pkg-config libssl-dev python3 colordiff

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

ENV PATH=$PATH:/usr/lib/ccache:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
RUN  /usr/sbin/update-ccache-symlinks
RUN git clone --depth 1 --branch llvmorg-19.1.7 https://github.com/llvm/llvm-project.git /src/llvm-final
RUN mkdir -p /src/llvm-final/build
RUN cmake -GNinja -S /src/llvm-final/llvm -B /src/llvm-final/build \
        -DCMAKE_BUILD_TYPE="MinSizeRel" \
        -DCMAKE_CXX_STANDARD=14 \
        -DLLVM_ENABLE_PROJECTS="clang;lld" \
        -DLLVM_TARGETS_TO_BUILD="RISCV;AArch64" \
        -DLLVM_PARALLEL_COMPILE_JOBS=$(nproc) \
        -DLLVM_PARALLEL_LINK_JOBS=2 \
        -DLLVM_CCACHE_BUILD=1 \
        -DLLVM_ENABLE_ASSERTIONS=On \
        -DCMAKE_C_COMPILER_LAUNCHER=sccache \
        -DCMAKE_CXX_COMPILER_LAUNCHER=sccache
RUN cmake --build /src/llvm-final/build