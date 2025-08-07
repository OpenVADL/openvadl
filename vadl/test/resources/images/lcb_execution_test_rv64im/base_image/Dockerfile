FROM ghcr.io/openvadl/lcb-test-base@sha256:c3cce3f0bc3bfe905668cd3528151c0465ad12aaca0fdd43f8e1bf39dc1c3def AS riscv-toolchain

# LLVM
FROM ghcr.io/openvadl/llvm19-base@sha256:f753f892f576f82f477d2c597d84207bc874d89ecda1c7644dbaf95f81439cb5
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
