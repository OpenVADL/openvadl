FROM ghcr.io/openvadl/lcb-test-base@sha256:0c91110246c88996b69bdffae4510f86959fabf57ebda51662a04a23b8b86aae AS riscv-toolchain

# Build elf updater tool
FROM rust:1.87 AS rust-builder
WORKDIR /usr/src

RUN apt-get update && apt-get install -y git
RUN git clone https://github.com/kper/elf_machine_updater.git
WORKDIR /usr/src/elf_machine_updater
RUN cargo build --release

# LLVM
FROM ghcr.io/openvadl/llvm19-base@sha256:743dc3df521b6721ee4a74126aff198f40b6fc585d2c06b76869cabbb3d2c015
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
