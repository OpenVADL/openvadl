FROM ghcr.io/openvadl/lcb-execution-test-aarch-base-image@sha256:d01ae9c63f13b38a504b140f76eb5f26b462f6d2c07ac3882b33cbae5ba52db2
ARG TARGET
ARG SCCACHE_REDIS_ENDPOINT
ARG UPSTREAM_BUILD_TARGET
ARG UPSTREAM_CLANG_TARGET
ARG SPIKE_TARGET
ARG ABI
WORKDIR /llvm/build
ENV TARGET=${TARGET}
ENV UPSTREAM_CLANG_TARGET=${UPSTREAM_CLANG_TARGET}
ENV LLVM_UPSTREAM_TARGETS=${UPSTREAM_BUILD_TARGET}
ENV SCCACHE_REDIS_ENDPOINT=${SCCACHE_REDIS_ENDPOINT}
ENV SPIKE_TARGET=${SPIKE_TARGET}
ENV ABI=${ABI}
ENV PATH=/opt/aarch64/bin:/opt/qemu/bin:$PATH
COPY . /src
WORKDIR /src
RUN make && sccache -s
RUN mkdir /output
CMD sh /work/compile.sh