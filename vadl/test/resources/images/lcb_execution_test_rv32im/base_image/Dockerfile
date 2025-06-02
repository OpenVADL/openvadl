FROM ghcr.io/openvadl/lcb-test-base@sha256:88d6f4e766b96ce74bd0b4fa9a5608a6eff0331ff21fe14eb16d30b9946723bf AS riscv-toolchain

# LLVM
FROM ghcr.io/openvadl/llvm19-base@sha256:da90881797b77a7d4b626d89a7c26d8a3baab9ae0bd5660fbf2544a255ce1e03
RUN apt update -y
RUN apt-get install -y device-tree-compiler python3 python3-pip python3-pandas vim libglib2.0-dev libfdt-dev libpixman-1-dev zlib1g-dev libslirp0 time
COPY --from=riscv-toolchain /opt/riscv /opt/riscv
COPY --from=riscv-toolchain /opt/qemu /opt/qemu
COPY --from=riscv-toolchain /usr/local/bin/ /usr/local/bin
WORKDIR /work
COPY helper /helper
COPY lcb_wrapper.sh .
COPY llvm.sh .
COPY compile.sh .
COPY filecheck.sh .
COPY upstream.sh .
COPY compare.sh .
COPY elf_machine_updater .
COPY asm.sh .
