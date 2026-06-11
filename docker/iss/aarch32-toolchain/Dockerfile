FROM ubuntu:22.04 AS build

WORKDIR /work

RUN apt update && apt install -y wget tar xz-utils

RUN mkdir -p /opt/aarch32-linux /opt/aarch32-elf

RUN set -ex; \
    ARCH=$(uname -m); \
    case "$ARCH" in \
        x86_64) SUFFIX_LINUX="x86_64-arm-none-linux-gnueabihf.tar.xz"; SUFFIX_ELF="x86_64-arm-none-eabi.tar.xz" ;; \
        aarch64) SUFFIX_LINUX="aarch64-arm-none-linux-gnueabihf.tar.xz"; SUFFIX_ELF="aarch64-arm-none-eabi.tar.xz" ;; \
        *) echo "Unsupported architecture: $ARCH" >&2; exit 1 ;; \
    esac; \
    # Download and extract Linux version
    wget -O linux.tar.xz "https://developer.arm.com/-/media/Files/downloads/gnu/14.2.rel1/binrel/arm-gnu-toolchain-14.2.rel1-$SUFFIX_LINUX"; \
    tar --strip-components=1 -xf linux.tar.xz -C /opt/aarch32-linux; \
    # Download and extract bare-metal version
    wget -O elf.tar.xz "https://developer.arm.com/-/media/Files/downloads/gnu/14.2.rel1/binrel/arm-gnu-toolchain-14.2.rel1-$SUFFIX_ELF"; \
    tar --strip-components=1 -xf elf.tar.xz -C /opt/aarch32-elf

FROM ubuntu:22.04 AS final
COPY --from=build /opt/aarch32-linux /opt/aarch32-linux
COPY --from=build /opt/aarch32-elf /opt/aarch32-elf

ENV PATH=/opt/aarch32-elf/bin:/opt/aarch32-linux/bin:$PATH

# Testing toolchain commands

RUN arm-none-linux-gnueabihf-addr2line --help; \
 arm-none-linux-gnueabihf-ar --help; \
 arm-none-linux-gnueabihf-as --help; \
 arm-none-linux-gnueabihf-c++ --help; \
 arm-none-linux-gnueabihf-c++filt --help; \
 arm-none-linux-gnueabihf-cpp --help; \
 arm-none-linux-gnueabihf-elfedit --help; \
 arm-none-linux-gnueabihf-g++ --help; \
 arm-none-linux-gnueabihf-gcc --help; \
 arm-none-linux-gnueabihf-gcc-ar --help; \
 arm-none-linux-gnueabihf-gcc-nm --help; \
 arm-none-linux-gnueabihf-gcc-ranlib --help; \
 arm-none-linux-gnueabihf-gcov --help; \
 arm-none-linux-gnueabihf-gcov-dump --help; \
 arm-none-linux-gnueabihf-gcov-tool --help; \
 arm-none-linux-gnueabihf-gdb --help; \
 arm-none-linux-gnueabihf-gprof --help; \
 arm-none-linux-gnueabihf-ld --help; \
 arm-none-linux-gnueabihf-ld.bfd --help; \
 arm-none-linux-gnueabihf-lto-dump --help; \
 arm-none-linux-gnueabihf-nm --help; \
 arm-none-linux-gnueabihf-objcopy --help; \
 arm-none-linux-gnueabihf-objdump --help; \
 arm-none-linux-gnueabihf-ranlib --help; \
 arm-none-linux-gnueabihf-readelf --help; \
 arm-none-linux-gnueabihf-run --help; \
 arm-none-linux-gnueabihf-size --help; \
 arm-none-linux-gnueabihf-strings --help; \
 arm-none-linux-gnueabihf-strip --help

RUN arm-none-eabi-addr2line --help; \
 arm-none-eabi-ar --help; \
 arm-none-eabi-as --help; \
 arm-none-eabi-c++ --help; \
 arm-none-eabi-c++filt --help; \
 arm-none-eabi-cpp --help; \
 arm-none-eabi-elfedit --help; \
 arm-none-eabi-g++ --help; \
 arm-none-eabi-gcc --help; \
 arm-none-eabi-gcc-ar --help; \
 arm-none-eabi-gcc-nm --help; \
 arm-none-eabi-gcc-ranlib --help; \
 arm-none-eabi-gcov --help; \
 arm-none-eabi-gcov-dump --help; \
 arm-none-eabi-gcov-tool --help; \
 arm-none-eabi-gdb --help; \
 arm-none-eabi-gprof --help; \
 arm-none-eabi-ld --help; \
 arm-none-eabi-ld.bfd --help; \
 arm-none-eabi-lto-dump --help; \
 arm-none-eabi-nm --help; \
 arm-none-eabi-objcopy --help; \
 arm-none-eabi-objdump --help; \
 arm-none-eabi-ranlib --help; \
 arm-none-eabi-readelf --help; \
 arm-none-eabi-run --help; \
 arm-none-eabi-size --help; \
 arm-none-eabi-strings --help; \
 arm-none-eabi-strip --help
