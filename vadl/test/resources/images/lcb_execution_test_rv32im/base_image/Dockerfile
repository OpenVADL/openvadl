FROM ghcr.io/openvadl/lcb-test-base@sha256:c3cce3f0bc3bfe905668cd3528151c0465ad12aaca0fdd43f8e1bf39dc1c3def AS riscv-toolchain

# Build elf updater tool
FROM rust:1.87 AS rust-builder
WORKDIR /usr/src

RUN apt-get update && apt-get install -y git
RUN git clone https://github.com/kper/elf_machine_updater.git
WORKDIR /usr/src/elf_machine_updater
RUN cargo build --release

# LLVM
FROM ghcr.io/openvadl/llvm19-base@sha256:da90881797b77a7d4b626d89a7c26d8a3baab9ae0bd5660fbf2544a255ce1e03
RUN apt update -y
RUN apt-get install -y device-tree-compiler python3 python3-pip python3-pandas vim libglib2.0-dev libfdt-dev libpixman-1-dev zlib1g-dev libslirp0 time
COPY --from=riscv-toolchain /opt/riscv /opt/riscv
COPY --from=riscv-toolchain /opt/qemu /opt/qemu
COPY --from=riscv-toolchain /usr/local/bin/ /usr/local/bin

COPY --from=rust-builder /usr/src/elf_machine_updater/target/release/elf_machine_updater /work/elf_machine_updater

WORKDIR /work
COPY helper /helper
COPY lcb_wrapper.sh .
COPY llvm.sh .
COPY compile.sh .
COPY filecheck.sh .
COPY upstream.sh .
COPY compare.sh .
COPY asm.sh .
COPY lcb_integration.sh .
