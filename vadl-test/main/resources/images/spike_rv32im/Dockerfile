FROM kper1337/riscv32-spike-lcb-test@sha256:a2796985bb2c491a8922710b1097837636b0c182539e2f8dde3a6270f19cf060
ARG TARGET
ARG SCCACHE_REDIS_ENDPOINT
ARG UPSTREAM_BUILD_TARGET
ARG UPSTREAM_CLANG_TARGET
ARG SPIKE_TARGET
WORKDIR /llvm/build
ENV TARGET=${TARGET}
ENV UPSTREAM_CLANG_TARGET=${UPSTREAM_CLANG_TARGET}
ENV LLVM_UPSTREAM_TARGETS=${UPSTREAM_BUILD_TARGET}
ENV SCCACHE_REDIS_ENDPOINT=${SCCACHE_REDIS_ENDPOINT}
ENV SPIKE_TARGET=${SPIKE_TARGET}
COPY . /src
WORKDIR /src
RUN mv /opt/sccache/sccache /usr/bin/sccache
RUN make && sccache -s
RUN mkdir /output

CMD /src/llvm-final/build/bin/clang --target=${TARGET} -S -c /src/inputs/$INPUT -o /tmp/main.s && chmod 777 /tmp/main.s && cat /tmp/main.s && sh /work/lcb_wrapper.sh