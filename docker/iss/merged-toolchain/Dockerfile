FROM ghcr.io/openvadl/aarch32-toolchain@sha256:1cd5737436cf7b584bc7af4245e2c2932b6b00022b9c5cd598a7ff868531351c AS aarch32
FROM ghcr.io/openvadl/aarch64-toolchain@sha256:68f072ee959a07586a2a761d9ba354d0f218cf27c756277510c01579ea701c13 AS aarch64
FROM ghcr.io/openvadl/ppc64-toolchain:latest AS ppc64
FROM ghcr.io/openvadl/riscv-toolchain:medany-rv32i-rv32im-rv32impriv-rv64i-rv64im-rv64impriv AS riscv

FROM ubuntu:22.04 AS build
COPY --from=riscv /opt/riscv /opt/riscv
COPY --from=ppc64 /opt/ppc64 /opt/ppc64
COPY --from=aarch32 /opt/aarch32-linux /opt/aarch32-linux
COPY --from=aarch32 /opt/aarch32-elf /opt/aarch32-elf
COPY --from=aarch64 /opt/aarch64-linux /opt/aarch64-linux
COPY --from=aarch64 /opt/aarch64-elf /opt/aarch64-elf

ENV TOOLCHAIN_PATH=/opt/riscv/bin:\
/opt/aarch32-elf/bin:\
/opt/aarch32-linux/bin:\
/opt/aarch64-elf/bin:\
/opt/aarch64-linux/bin

ENV PATH=${TOOLCHAIN_PATH}:$PATH
