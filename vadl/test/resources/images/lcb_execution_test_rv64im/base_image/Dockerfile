FROM ghcr.io/openvadl/qemu-riscv64-base@sha256:259f08ecb93dc1f575b37568c803f193fbfabe8a9490bfa4dffbcea397ca5eae AS riscv-toolchain

# LLVM
FROM ghcr.io/openvadl/llvm17-base@sha256:26659363db3bd278f54fb6d81581038d270aaaee859d839188063545d50d81ae
RUN apt update -y
RUN apt-get install -y device-tree-compiler python3 python3-pip python3-pandas vim libglib2.0-dev libfdt-dev libpixman-1-dev zlib1g-dev
COPY --from=riscv-toolchain /opt/riscv /opt/riscv-cross
COPY --from=riscv-toolchain /usr/local/bin/ /usr/local/bin
WORKDIR /work
COPY helper /helper
COPY lcb_wrapper.sh .
COPY llvm.sh .
COPY compile.sh .
COPY filecheck.sh .
