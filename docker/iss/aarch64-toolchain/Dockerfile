FROM ubuntu:22.04 AS build

WORKDIR /work

RUN apt update && apt install -y wget tar xz-utils

RUN mkdir -p /opt/aarch64-linux /opt/aarch64-elf

RUN set -ex; \
    ARCH=$(uname -m); \
    case "$ARCH" in \
        x86_64) SUFFIX_LINUX="x86_64-aarch64-none-linux-gnu.tar.xz"; SUFFIX_ELF="x86_64-aarch64-none-elf.tar.xz" ;; \
        aarch64) SUFFIX_LINUX="aarch64-aarch64-none-linux-gnu.tar.xz"; SUFFIX_ELF="aarch64-aarch64-none-elf.tar.xz" ;; \
        *) echo "Unsupported architecture: $ARCH" >&2; exit 1 ;; \
    esac; \
    # Download and extract Linux version
    wget -O linux.tar.xz "https://developer.arm.com/-/media/Files/downloads/gnu/14.2.rel1/binrel/arm-gnu-toolchain-14.2.rel1-$SUFFIX_LINUX"; \
    tar --strip-components=1 -xf linux.tar.xz -C /opt/aarch64-linux; \
    # Download and extract bare-metal version
    wget -O elf.tar.xz "https://developer.arm.com/-/media/Files/downloads/gnu/14.2.rel1/binrel/arm-gnu-toolchain-14.2.rel1-$SUFFIX_ELF"; \
    tar --strip-components=1 -xf elf.tar.xz -C /opt/aarch64-elf

FROM ubuntu:22.04 AS final
COPY --from=build /opt/aarch64-linux /opt/aarch64-linux
COPY --from=build /opt/aarch64-elf /opt/aarch64-elf

ENV PATH=/opt/aarch64-elf/bin:/opt/aarch64-linux/bin:$PATH

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

RUN aarch64-none-elf-addr2line --help; \
 aarch64-none-elf-ar --help; \
 aarch64-none-elf-as --help; \
 aarch64-none-elf-c++ --help; \
 aarch64-none-elf-c++filt --help; \
 aarch64-none-elf-cpp --help; \
 aarch64-none-elf-elfedit --help; \
 aarch64-none-elf-g++ --help; \
 aarch64-none-elf-gcc --help; \
 aarch64-none-elf-gcc-ar --help; \
 aarch64-none-elf-gcc-nm --help; \
 aarch64-none-elf-gcc-ranlib --help; \
 aarch64-none-elf-gcov --help; \
 aarch64-none-elf-gcov-dump --help; \
 aarch64-none-elf-gcov-tool --help; \
 aarch64-none-elf-gdb --help; \
 aarch64-none-elf-gprof --help; \
 aarch64-none-elf-ld --help; \
 aarch64-none-elf-ld.bfd --help; \
 aarch64-none-elf-lto-dump --help; \
 aarch64-none-elf-nm --help; \
 aarch64-none-elf-objcopy --help; \
 aarch64-none-elf-objdump --help; \
 aarch64-none-elf-ranlib --help; \
 aarch64-none-elf-readelf --help; \
 aarch64-none-elf-run --help; \
 aarch64-none-elf-size --help; \
 aarch64-none-elf-strings --help; \
 aarch64-none-elf-strip --help