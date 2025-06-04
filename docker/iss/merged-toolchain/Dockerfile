FROM ghcr.io/openvadl/aarch64-toolchain@sha256:68f072ee959a07586a2a761d9ba354d0f218cf27c756277510c01579ea701c13 AS aarch64
FROM ghcr.io/openvadl/riscv-toolchain:medany-rv32i-rv32im-rv32impriv-rv64i-rv64im-rv64impriv AS riscv

FROM ubuntu:22.04 AS build
COPY --from=riscv /opt/riscv /opt/riscv
COPY --from=aarch64 /opt/aarch64-linux /opt/aarch64-linux
COPY --from=aarch64 /opt/aarch64-elf /opt/aarch64-elf

ENV PATH=/opt/riscv/bin:/opt/aarch64-elf/bin:/opt/aarch64-linux/bin:$PATH
