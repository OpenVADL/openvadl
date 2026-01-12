# ────────────────────────────
# Build Container with All JDKs
# ────────────────────────────
FROM ubuntu:24.04 AS builder

ARG JAVA_VERSION=25.0.1
ARG ZULU_BUILD=25.30.17-ca

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    curl unzip tar && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /opt

# ----------------------------
# Download & Extract JDKs
# ----------------------------

# Target JDKs for cross-jlink
ARG T_LINUX_X64=zulu${ZULU_BUILD}-jdk${JAVA_VERSION}-linux_x64.tar.gz
ARG T_LINUX_ARM64=zulu${ZULU_BUILD}-jdk${JAVA_VERSION}-linux_aarch64.tar.gz
ARG T_WIN_X64=zulu${ZULU_BUILD}-jdk${JAVA_VERSION}-win_x64.zip
ARG T_WIN_ARM64=zulu${ZULU_BUILD}-jdk${JAVA_VERSION}-win_aarch64.zip
ARG T_MACOS_X64=zulu${ZULU_BUILD}-jdk${JAVA_VERSION}-macosx_x64.zip
ARG T_MACOS_ARM64=zulu${ZULU_BUILD}-jdk${JAVA_VERSION}-macosx_aarch64.zip

RUN mkdir -p /jdks

# Linux x64
RUN curl -L https://cdn.azul.com/zulu/bin/${T_LINUX_X64} -o ${T_LINUX_X64} && \
    tar -xzf ${T_LINUX_X64} && \
    mv *linux_x64 /jdks/jdk-linux-x64

# Linux ARM64
RUN curl -L https://cdn.azul.com/zulu/bin/${T_LINUX_ARM64} -o ${T_LINUX_ARM64} && \
    tar -xzf ${T_LINUX_ARM64} && \
    mv *linux_aarch64 /jdks/jdk-linux-arm64

# Windows x64
RUN curl -L https://cdn.azul.com/zulu/bin/${T_WIN_X64} -o ${T_WIN_X64} && \
    unzip -q ${T_WIN_X64} && \
    mv *win_x64 /jdks/jdk-win-x64

RUN curl -L https://cdn.azul.com/zulu/bin/${T_WIN_ARM64} -o ${T_WIN_ARM64} && \
    unzip -q ${T_WIN_ARM64} && \
    mv *win_aarch64 /jdks/jdk-win-arm64

# macOS x64
RUN curl -L https://cdn.azul.com/zulu/bin/${T_MACOS_X64} -o ${T_MACOS_X64} && \
    unzip -q ${T_MACOS_X64} && \
    mv *macosx_x64 /jdks/jdk-macos-x64

# macOS ARM64
RUN curl -L https://cdn.azul.com/zulu/bin/${T_MACOS_ARM64} -o ${T_MACOS_ARM64} && \
    unzip -q ${T_MACOS_ARM64} && \
    mv *macosx_aarch64 /jdks/jdk-macos-arm64

# Clean up apt lists
RUN rm -rf /var/lib/apt/lists/*

# ───────────────────────────
# Final Runtime Builder Image
# ───────────────────────────
FROM ubuntu:24.04 AS runtime

RUN apt-get update && apt-get install -y \
    binutils && \
    rm -rf /var/lib/apt/lists/*

# Copy all JDKs in a single directory
COPY --from=builder /jdks /jdks

# Set up host JDK based on architecture
RUN ARCH=$(uname -m) && \
    if [ "$ARCH" = "x86_64" ]; then \
        ln -s /jdks/jdk-linux-x64 /jdks/jdk-host; \
    elif [ "$ARCH" = "aarch64" ]; then \
        ln -s /jdks/jdk-linux-arm64 /jdks/jdk-host; \
    else \
        echo "Unsupported architecture: $ARCH" && exit 1; \
    fi

ENV PATH="/jdks/jdk-host/bin:${PATH}"

WORKDIR /src
CMD ["bash"]