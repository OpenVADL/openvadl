FROM ghcr.io/openvadl/aarch64-toolchain@sha256:1989ceedbf2c4ffb0b274ace03e3211f88bf1c24f9da462e22a990ed1617ed5e as aarch64
FROM ghcr.io/openvadl/riscv-toolchain:medany-rv32i-rv32im-rv32impriv-rv64i-rv64im-rv64impriv as riscv

FROM ubuntu:22.04 AS build
COPY --from=riscv /opt/riscv /opt/riscv
COPY --from=riscv /usr/local/bin/ /usr/local/bin
COPY --from=aarch64 /opt/aarch64 /opt/aarch64