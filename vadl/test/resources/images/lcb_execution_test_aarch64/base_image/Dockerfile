FROM ghcr.io/openvadl/qemu-base@sha256:a5a09dbc89913461f38eb206ceb7dbb1d3b0969355c7cd916cf50159dcbe6900 AS base

# LLVM
FROM ghcr.io/openvadl/llvm19-base@sha256:da90881797b77a7d4b626d89a7c26d8a3baab9ae0bd5660fbf2544a255ce1e03
RUN apt update -y
RUN apt-get install -y device-tree-compiler python3 python3-pip python3-pandas vim libglib2.0-dev libfdt-dev libpixman-1-dev zlib1g-dev libslirp0 time
COPY --from=base /opt/riscv /opt/riscv-cross
COPY --from=base /usr/local/bin/ /usr/local/bin
WORKDIR /work
COPY helper /helper
COPY lcb_wrapper.sh .
COPY llvm.sh .
COPY compile.sh .
COPY filecheck.sh .
