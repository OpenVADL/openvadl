# Base build stage for all architectures
FROM ubuntu:22.04 AS build

WORKDIR /work

RUN apt update
RUN apt install -y wget tar xz-utils

WORKDIR /work
RUN mkdir /opt/aarch64

RUN set -ex; \
    ARCH=$(uname -m); \
    case "$ARCH" in \
        x86_64) SUFFIX="x86_64-aarch64-none-linux-gnu.tar.xz" ;; \
        aarch64) SUFFIX="aarch64-aarch64-none-linux-gnu.tar.xz" ;; \
        *) echo "Unsupported architecture: $ARCH" >&2; exit 1 ;; \
    esac; \
    wget -O /work/output.tar.xz "https://developer.arm.com/-/media/Files/downloads/gnu/14.2.rel1/binrel/arm-gnu-toolchain-14.2.rel1-$SUFFIX"; \
    tar --strip-components=1 -xf /work/output.tar.xz -C /opt/aarch64

FROM ubuntu:22.04 AS final
COPY --from=build /opt/aarch64 /opt/aarch64

ENV PATH=/opt/aarch64/bin:$PATH

RUN apt update && \
    apt install -y python3  \
    python3-pip libmpc-dev libmpfr-dev libgmp-dev build-essential  \
    gperf libtool patchutils bc zlib1g-dev libexpat-dev       \
    libglib2.0-dev libslirp-dev

# Testing toolchain commands

RUN aarch64-none-linux-gnu-addr2line --help; \
 aarch64-none-linux-gnu-ar --help; \
 aarch64-none-linux-gnu-as --help; \
 aarch64-none-linux-gnu-c++ --help; \
 aarch64-none-linux-gnu-c++filt --help; \
 aarch64-none-linux-gnu-cpp --help; \
 aarch64-none-linux-gnu-elfedit --help; \
 aarch64-none-linux-gnu-g++ --help; \
 aarch64-none-linux-gnu-gcc --help; \
 aarch64-none-linux-gnu-gcc-ar --help; \
 aarch64-none-linux-gnu-gcc-nm --help; \
 aarch64-none-linux-gnu-gcc-ranlib --help; \
 aarch64-none-linux-gnu-gcov --help; \
 aarch64-none-linux-gnu-gcov-dump --help; \
 aarch64-none-linux-gnu-gcov-tool --help; \
 aarch64-none-linux-gnu-gdb --help; \
 aarch64-none-linux-gnu-gprof --help; \
 aarch64-none-linux-gnu-ld --help; \
 aarch64-none-linux-gnu-ld.bfd --help; \
 aarch64-none-linux-gnu-lto-dump --help; \
 aarch64-none-linux-gnu-nm --help; \
 aarch64-none-linux-gnu-objcopy --help; \
 aarch64-none-linux-gnu-objdump --help; \
 aarch64-none-linux-gnu-ranlib --help; \
 aarch64-none-linux-gnu-readelf --help; \
 aarch64-none-linux-gnu-run --help; \
 aarch64-none-linux-gnu-size --help; \
 aarch64-none-linux-gnu-strings --help; \
 aarch64-none-linux-gnu-strip --help