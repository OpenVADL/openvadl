FROM ubuntu
ARG TARGET

RUN apt-get update -y
RUN apt-get install -y make cmake ninja-build build-essential ccache git curl pkg-config libssl-dev python3 colordiff

ENV PATH=/opt/sccache:$PATH
RUN mkdir /opt/sccache
RUN set -ex; \
    ARCH=$(uname -m); \
    case "$ARCH" in \
        x86_64) SUFFIX="x86_64-unknown-linux-musl" ;; \
        aarch64) SUFFIX="aarch64-unknown-linux-musl" ;; \
        *) echo "Unsupported architecture: $ARCH" >&2; exit 1 ;; \
    esac; \
    curl -L https://github.com/mozilla/sccache/releases/download/v0.8.2/sccache-v0.8.2-$SUFFIX.tar.gz \
    | tar -xz -C /opt/sccache --strip-components=1
RUN mv /opt/sccache/sccache /usr/bin/sccache
ENV PATH=/usr/lib/ccache:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
RUN  /usr/sbin/update-ccache-symlinks
RUN git clone --depth 1 --branch llvmorg-17.0.6 https://github.com/llvm/llvm-project.git /src/llvm-final
COPY inputs/ /inputs
COPY output/lcb /src/
COPY run.sh /src/run.sh
WORKDIR /src
RUN --mount=type=cache,target=/root/.cache/sccache TARGET=$TARGET make
ENV LLVM_SOURCE_PATH=/src/llvm-final/
RUN chmod 777 /src/run.sh
CMD /src/run.sh
