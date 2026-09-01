# syntax=docker/dockerfile:1.7
FROM ghcr.io/openvadl/lcb-execution-test-rv64im-base-image@sha256:3cd1cfa0fd9db23ba514e38c9321add4e80f1da70bee0424e38d6af3db13d19a
ARG TARGET
ARG SCCACHE_REDIS_ENDPOINT
ARG UPSTREAM_BUILD_TARGET
ARG UPSTREAM_CLANG_TARGET
ARG SPIKE_TARGET
ARG ABI
LABEL key=VADL_TEST_CONTAINER
WORKDIR /llvm/build
ENV TARGET=${TARGET}
ENV UPSTREAM_CLANG_TARGET=${UPSTREAM_CLANG_TARGET}
ENV LLVM_UPSTREAM_TARGETS=${UPSTREAM_BUILD_TARGET}
ENV SPIKE_TARGET=${SPIKE_TARGET}
ENV ABI=${ABI}
ENV SCCACHE_DIR=/root/.cache/sccache
ENV PATH=/opt/riscv/bin:/opt/qemu/bin:$PATH
COPY . /src
WORKDIR /src
RUN --mount=type=cache,target=/root/.cache/sccache sccache --start-server && make && sccache -s
RUN mkdir /output
CMD sh /work/compile.sh
