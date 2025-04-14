# syntax=docker/dockerfile:1.6.0
#############################################################
# Build aarch64 libc stdlib (will be used with generic clang)
#############################################################
FROM alpine:3.19 as builder-aarch64
ARG num_jobs=1

RUN apk add --no-cache python3 make bash git \
	clang lld binutils-aarch64-none-elf

# Build musl

RUN git clone https://git.musl-libc.org/git/musl /musl-src
WORKDIR /musl-src

RUN rm -rf src/*/aarch64
RUN sed -i 's/__asm__ __volatile__ ("" : "+w"(x));/__asm__ __volatile__ ("" : "+r"(x));/g' arch/aarch64/fp_arch.h
# Prevent infinite recursion inside fma() when clang replaces `x*y+z` with a recursive `fma(x,y,z)`
RUN sed -i 's/return\s*x\s*\*\s*y\s*+\s*z\s*;/{double p = x*y; return p+z;}/' src/math/fma.c

RUN CC=clang \
	CFLAGS="-target aarch64-none-elf -march=armv8-a+nofp+nosimd" \
	CROSS_COMPILE="aarch64-none-elf-" \
	./configure \
		--target=aarch64-none-elf \
		--prefix=/opt/aarch64-cross \
		--sysconfdir=/opt/aarch64-cross/etc \
		--mandir=/unused/man \
		--infodir=/unused/info \
		--localstatedir=/unused/var \
		--enable-static \
		--disable-shared

RUN make install -j $num_jobs

# Build LLVM compilter-rt bulitins for softfp functions

COPY ./aarch64-compiler-rt /llvm-builtins
WORKDIR /llvm-builtins
RUN make install PREFIX=/opt/aarch64-cross -j $num_jobs

# These are the needed flags
# ENV CFLAGS  ="-target aarch64-none-elf -march=armv8-a+nofp+nosimd"
# ENV CPPFLAGS="-target aarch64-none-elf -march=armv8-a+nofp+nosimd -I /opt/aarch64-cross/include"
# ENV LDFLAGS ="-target aarch64-none-elf -march=armv8-a+nofp+nosimd -L /opt/aarch64-cross/lib -lc -lllvm-builtins"


#############################################################
# Build riscv compiler (will be used with generic clang)
#############################################################
FROM debian:12 as builder-riscv-toolchain

ARG num_jobs=1

RUN apt update && apt upgrade -y
RUN apt install -y build-essential autoconf automake autotools-dev python3 python3-pip libmpc-dev libmpfr-dev libgmp-dev gawk bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev ninja-build git cmake libglib2.0-dev curl device-tree-compiler
WORKDIR /root

RUN git clone https://github.com/riscv-collab/riscv-gnu-toolchain
WORKDIR riscv-gnu-toolchain
RUN git submodule update --init --depth=1 -- gcc
RUN git submodule update --init --depth=1 -- glibc
RUN git submodule update --init --depth=1 -- binutils
RUN ./configure --with-arch=rv32iam --with-abi=ilp32 --prefix=/opt/riscv-cross && make linux -j $num_jobs && make clean



#############################################################
# Build spike
#############################################################
FROM debian:12 as builder-spike

ARG num_jobs=1

RUN apt update && apt upgrade -y
RUN apt install -y build-essential autoconf automake autotools-dev python3 python3-pip libmpc-dev libmpfr-dev libgmp-dev gawk bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev ninja-build git cmake libglib2.0-dev curl device-tree-compiler
WORKDIR /root

RUN git clone https://github.com/riscv-software-src/riscv-isa-sim
WORKDIR riscv-isa-sim
RUN ./configure --prefix=/opt/spike && make -j $num_jobs && make install && make clean




#############################################################
# QEMU
#############################################################
FROM debian:12 as builder-qemu

ARG num_jobs=1

RUN apt update && apt upgrade -y
RUN apt install -y build-essential ninja-build libglib2.0-dev python3 wget python3-pip libseccomp-dev

WORKDIR /
RUN wget https://download.qemu.org/qemu-8.2.0.tar.xz
RUN tar -xf qemu-8.2.0.tar.xz
RUN mkdir /qemu-8.2.0/build
WORKDIR /qemu-8.2.0/build
RUN CFLAGS="$CFLAGS -O2" CXXFLAGS="$CXXFLAGS -O2" CPPFLAGS="$CPPFLAGS -O2" ../configure \
	--target-list=aarch64-linux-user,aarch64-softmmu,riscv32-linux-user,riscv32-softmmu \
	--audio-drv-list= \
	--enable-kvm \
	--disable-docs \
	--prefix=/opt/qemu

RUN make -j $num_jobs && make install && make clean

WORKDIR /
WORKDIR /qemu-8.2.0
RUN mkdir /qemu-8.2.0/build-nojit
WORKDIR /qemu-8.2.0/build-nojit
RUN CFLAGS="$CFLAGS -O2" CXXFLAGS="$CXXFLAGS -O2" CPPFLAGS="$CPPFLAGS -O2" ../configure \
	--target-list=aarch64-linux-user,aarch64-softmmu,riscv32-linux-user,riscv32-softmmu \
	--audio-drv-list= \
	--enable-kvm \
	--disable-docs \
	--enable-tcg-interpreter \
	--prefix=/opt/qemu-nojit

RUN make -j $num_jobs && make install && make clean




#############################################################
# gem5 ARM + RISCV
#############################################################
FROM debian:12 as builder-gem5

ARG num_jobs=1

RUN apt update && apt upgrade -y
RUN apt install -y build-essential git m4 scons zlib1g zlib1g-dev libgoogle-perftools-dev python3-dev

WORKDIR /
RUN git clone https://github.com/gem5/gem5 --branch v23.1.0.0
WORKDIR /gem5
COPY <<EOF build_opts/ARM_RISCV
RUBY=y
RUBY_PROTOCOL_MESI_TWO_LEVEL=y
BUILD_ISA=y
USE_ARM_ISA=y
USE_RISCV_ISA=y
EOF
RUN scons build/ARM_RISCV/gem5.fast -j $num_jobs

#############################################################
# Main runtime image
#############################################################

FROM debian:12

ARG ACCESS_TOKEN

RUN apt-get update && apt-get upgrade -y
RUN apt-get install -y make clang python3 binutils-aarch64-linux-gnu
RUN apt-get install -y lld libmpc-dev libmpfr-dev libgmp-dev

COPY --from=builder-aarch64 /opt/aarch64-cross /opt/aarch64-cross
COPY --from=builder-spike /opt/spike /opt/spike
COPY --from=builder-riscv-toolchain /opt/riscv-cross /opt/riscv-cross
COPY --from=builder-qemu /opt/qemu /opt/qemu
COPY --from=builder-qemu /opt/qemu-nojit /opt/qemu-nojit
COPY --from=builder-gem5 /gem5/build/ARM_RISCV/gem5.fast /opt/gem5/build/ARM_RISCV/gem5.fast

RUN apt-get install -y libglib2.0-dev python3-pandas build-essential device-tree-compiler libgoogle-perftools-dev curl yq unzip openjdk-21-jdk git-all scala apt-transport-https gnupg cmake ninja-build vim
RUN apt-get install -y ccache libboost-all-dev

RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import
RUN chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg
RUN apt-get update
RUN apt-get install sbt

# Install Verilator
RUN apt-get install -y numactl build-essential autoconf automake autotools-dev gawk bison flex cmake help2man
WORKDIR /
RUN git clone --branch v5.008 https://github.com/verilator/verilator.git
WORKDIR /verilator
RUN autoconf
RUN ./configure
RUN make -j16
RUN make install
RUN verilator --version

ENV PATH="$PATH:/opt/riscv-cross/bin:/opt/spike/bin:/opt/gem5/build/ARM_RISCV/"

RUN /usr/sbin/update-ccache-symlinks
ENV PATH="/usr/lib/ccache:$PATH"

WORKDIR /
RUN git clone --depth 1 --branch llvmorg-17.0.6 https://github.com/llvm/llvm-project.git llvm
RUN mkdir /llvm/build

WORKDIR /
RUN git config --add --global url."https://${ACCESS_TOKEN}@ea.complang.tuwien.ac.at/".insteadOf "git@ea.complang.tuwien.ac.at:"
RUN git clone git@ea.complang.tuwien.ac.at:vadl/vadl.git vadl-src

WORKDIR /
