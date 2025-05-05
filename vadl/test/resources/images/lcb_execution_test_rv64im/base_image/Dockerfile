FROM ghcr.io/openvadl/qemu-riscv64-base@sha256:259f08ecb93dc1f575b37568c803f193fbfabe8a9490bfa4dffbcea397ca5eae AS riscv-toolchain

# Rust

FROM rust:latest AS builder-fail-if
RUN cargo install --git https://github.com/kper/fail_if fail_if

# LLVM
FROM ghcr.io/openvadl/llvm17-base@sha256:26659363db3bd278f54fb6d81581038d270aaaee859d839188063545d50d81ae
RUN apt update -y
RUN apt-get install -y device-tree-compiler python3 python3-pip python3-pandas vim
COPY --from=riscv-toolchain /opt/riscv /opt/riscv-cross
COPY --from=builder-fail-if /usr/local/cargo/bin/fail_if /usr/bin/fail_if
WORKDIR /work
COPY helper /helper
COPY lcb_wrapper.sh .
COPY llvm.sh .
COPY compile.sh .
COPY filecheck.sh .
