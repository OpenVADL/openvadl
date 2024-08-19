FROM ubuntu:24.04 as rv-build

ENV RISCV=/opt/riscv
ENV PATH=$RISCV/bin:$PATH
WORKDIR /work

RUN apt update
RUN apt install -y autoconf automake autotools-dev curl python3 python3-pip libmpc-dev libmpfr-dev libgmp-dev gawk build-essential bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev ninja-build git cmake libglib2.0-dev libslirp-dev
RUN apt install -y git

RUN git clone https://github.com/riscv-collab/riscv-gnu-toolchain

# We build the bare-metal (no linux) toolchain with the multilibs (newlib)
RUN mkdir riscv-gnu-toolchain/build-rv64i-lp64 && \
    cd riscv-gnu-toolchain/build-rv64i-lp64 && \
    ../configure --prefix=/opt/riscv --enable-multilib && \
    make

FROM ubuntu:24.04
COPY --from=rv-build /opt/riscv/bin /bin
WORKDIR /work