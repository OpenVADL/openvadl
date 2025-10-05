FROM ubuntu:22.04 AS build

WORKDIR /work

RUN apt update && \
    apt install -y autoconf automake autotools-dev curl python3 python3-pip libmpc-dev libmpfr-dev \
    libgmp-dev gawk build-essential bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev \
    ninja-build git cmake libglib2.0-dev libslirp-dev wget

ENV BINUTILS_VERSION=2.45

# mirror for https://ftp.gnu.org/gnu/binutils/binutils-$BINUTILS_VERSION.tar.xz
RUN wget https://mirrors.kernel.org/gnu/binutils/binutils-$BINUTILS_VERSION.tar.xz && \
    tar -xf binutils-$BINUTILS_VERSION.tar.xz && \
    rm binutils-$BINUTILS_VERSION.tar.xz && \
    mv binutils-$BINUTILS_VERSION binutils

RUN mkdir binutils/build-ppc64 binutils/build-ppc64le

# powerpc64 toolchain
WORKDIR /work/binutils/build-ppc64
RUN ../configure --target=powerpc64-unknown-elf --prefix=/opt/ppc64
RUN make -j8 && make install

# powerpc64le toolchain
WORKDIR /work/binutils/build-ppc64le
RUN ../configure --target=powerpc64le-unknown-elf --prefix=/opt/ppc64
RUN make -j8 && make install

FROM ubuntu:22.04 AS ppc64-toolchain
COPY --from=build /opt/ppc64 /opt/ppc64
ENV PATH=/opt/ppc64/bin:$PATH

WORKDIR /work

RUN apt update && \
    apt install -y python3 \
    python3-pip libmpc-dev libmpfr-dev libgmp-dev build-essential \
    gperf libtool patchutils bc zlib1g-dev libexpat-dev \
    libglib2.0-dev libslirp-dev

# testing toolchain commands
RUN powerpc64-unknown-elf-as --help
RUN powerpc64-unknown-elf-ld --help
RUN powerpc64le-unknown-elf-as --help
RUN powerpc64le-unknown-elf-ld --help
