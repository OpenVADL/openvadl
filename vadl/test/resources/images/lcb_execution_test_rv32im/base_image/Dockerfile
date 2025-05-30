FROM ghcr.io/openvadl/lcb-test-base@sha256:37a256bc5916a01f2d5634457aa2ebed41419ffdad1af78a4dc1a8c1b4b3f267 AS riscv-toolchain

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
