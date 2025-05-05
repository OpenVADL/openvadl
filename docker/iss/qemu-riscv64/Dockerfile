FROM ghcr.io/openvadl/qemu-base@sha256:f4cb8676c5a3cdb4f886b8bbcde6bae07c3b03e9f8a78bc216a7358bdff2054b

WORKDIR /qemu/build
RUN ../configure --target-list=riscv64-softmmu
RUN make -j $(nproc)
RUN make install