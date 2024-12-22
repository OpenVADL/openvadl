FROM kper1337/riscv32-spike-lcb-test@sha256:863b172503da6b4d63a6f0364e1416de58d413d8bc4e39af76c56168f0c9eadc
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