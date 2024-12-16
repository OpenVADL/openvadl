# Base build stage for all architectures
FROM ubuntu:22.04 AS build

WORKDIR /work

RUN apt update
RUN apt install -y autoconf automake autotools-dev curl python3 python3-pip libmpc-dev libmpfr-dev  \
    libgmp-dev gawk build-essential bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev  \
    ninja-build git cmake libglib2.0-dev libslirp-dev git

RUN git clone https://github.com/riscv-collab/riscv-gnu-toolchain

RUN mkdir riscv-gnu-toolchain/build
WORKDIR /work/riscv-gnu-toolchain/build

RUN ../configure --prefix=/opt/riscv  \
    --with-cmodel=medany  \
    --with-arch=rv64i --with-abi=lp64  \
    --with-multilib-generator="\
    rv32i-ilp32--;\
    rv32im-ilp32--;\
    rv32im_zicsr-ilp32--;\
    rv64i-lp64--;\
    rv64im-lp64--;\
    rv64im_zicsr-lp64--"
RUN make

# Separate final stages for different architectures
FROM ubuntu:22.04 AS final
COPY --from=build /opt/riscv /opt/riscv

FROM final AS riscv-toolchain
ENV PATH=/opt/riscv/bin:$PATH

WORKDIR /work

RUN apt update && \
    apt install -y python3  \
    python3-pip libmpc-dev libmpfr-dev libgmp-dev build-essential  \
    gperf libtool patchutils bc zlib1g-dev libexpat-dev       \
    libglib2.0-dev libslirp-dev

# Testing toolchain commands
RUN riscv64-unknown-elf-addr2line --help
RUN riscv64-unknown-elf-ar --help
RUN riscv64-unknown-elf-as --help
RUN riscv64-unknown-elf-c++ --help
RUN riscv64-unknown-elf-c++filt --help
RUN riscv64-unknown-elf-cpp --help
RUN riscv64-unknown-elf-elfedit --help
RUN riscv64-unknown-elf-g++ --help
RUN riscv64-unknown-elf-gcc --help
RUN riscv64-unknown-elf-gcc-13.2.0 --help
RUN riscv64-unknown-elf-gcc-ar --help
RUN riscv64-unknown-elf-gcc-nm --help
RUN riscv64-unknown-elf-gcc-ranlib --help
RUN riscv64-unknown-elf-gcov --help
RUN riscv64-unknown-elf-gcov-dump --help
RUN riscv64-unknown-elf-gcov-tool --help
RUN riscv64-unknown-elf-gdb --help
RUN riscv64-unknown-elf-gprof --help
RUN riscv64-unknown-elf-ld --help
RUN riscv64-unknown-elf-ld.bfd --help
RUN riscv64-unknown-elf-lto-dump --help
RUN riscv64-unknown-elf-nm --help
RUN riscv64-unknown-elf-objcopy --help
RUN riscv64-unknown-elf-objdump --help
RUN riscv64-unknown-elf-ranlib --help
RUN riscv64-unknown-elf-readelf --help
RUN riscv64-unknown-elf-run --help
RUN riscv64-unknown-elf-size --help
RUN riscv64-unknown-elf-strings --help
RUN riscv64-unknown-elf-strip --help
RUN riscv64-unknown-elf-gcc --print-multi-lib