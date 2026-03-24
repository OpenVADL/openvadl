# syntax=docker/dockerfile:1.7
FROM ghcr.io/openvadl/lcb-execution-test-aarch64-base-image@sha256:78d418708da2656805f7bac1204a1adc55f5a72f749b6d2100e275e176c816cf
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
ENV PATH=/opt/aarch64/bin:/opt/qemu/bin:$PATH
COPY . /src
WORKDIR /src
RUN --mount=type=cache,target=/root/.cache/sccache sccache --start-server && make && sccache -s
RUN mkdir /output
CMD sh /work/compile.sh
