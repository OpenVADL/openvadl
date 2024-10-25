FROM kper1337/llvm17_base:v1
ARG TARGET
ARG SCCACHE_REDIS_ENDPOINT
WORKDIR /llvm/build
ENV TARGET=${TARGET}
ENV SCCACHE_REDIS_ENDPOINT=${SCCACHE_REDIS_ENDPOINT}
COPY . /src
WORKDIR /src
RUN mv /opt/sccache/sccache /usr/bin/sccache
RUN make && /opt/sccache/sccache -s
RUN mkdir /output
CMD /src/llvm-final/build/bin/clang --target=${TARGET} -S -c /src/input.c -o input.s